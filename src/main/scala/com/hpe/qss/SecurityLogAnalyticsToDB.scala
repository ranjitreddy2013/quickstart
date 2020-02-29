package com.hpe.qss

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.elasticsearch.hadoop.cfg.ConfigurationOptions
import com.mapr.db.spark.streaming.MapRDBSourceConfig


object SecurityLogAnalyticsToDB {
  def main(args: Array[String]) = {

    val config = ConfigFactory.load()

    val master = config.getString("spark.master")
    val appName = config.getString("spark.app.name")
    val elasticsearchHost = config.getString("spark.elasticsearch.host")
    val elasticsearchPort = config.getString("spark.elasticsearch.port")

    val outputMode = config.getString("spark.elasticsearch.output.mode")
    val destination = config.getString("spark.elasticsearch.data.source")
    val checkpointLocation = config.getString("spark.elasticsearch.checkpoint.location")
    val index = config.getString("spark.elasticsearch.index")
    val docType = config.getString("spark.elasticsearch.doc.type")
    val indexAndDocType = s"$index/$docType"
    var tableName: String = "/user/mapr/mytable"

    val spark: SparkSession = SparkSession.builder()
      .master(master)
      .appName(appName)
      .getOrCreate();


    import spark.implicits._

    val schema = new StructType()
      .add("received_at", TimestampType)
      .add("message", StringType)
      .add("path", StringType)
      .add("@timestamp", TimestampType)
      .add("auth_status", StringType)
      .add("type", StringType)
      .add("audit_type", StringType)
      .add("audit_sshd_ip", StringType)
      .add("@version", StringType)
      .add("msg", StringType)
      .add("host", StringType)
      .add("terminal", StringType)
      .add("received_from", StringType)

    val lines = spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "localhost")
      .option("failOnDataLoss", "false")
      .option("subscribePattern", "/tmp/stream:audit_log")
      .load()
      .select($"value" cast "string" as "json")
      .select(from_json($"json", schema) as "data")
      .select("data.received_at", "data.auth_status",
        "data.audit_sshd_ip", "data.msg", "data.received_from")

    val filteredLines = lines.filter($"audit_sshd_ip".isNotNull) //filter out rows that don't have source IP

    val windowedCounts = filteredLines
      .withWatermark("received_at", "10 minutes")
      .groupBy(
        window($"received_at", s"10 seconds", s"5 seconds"),
        $"audit_sshd_ip")
      .count()
    //.orderBy("window")


    val query = windowedCounts.select($"audit_sshd_ip" as "_id", $"count")
      .writeStream.format(MapRDBSourceConfig.Format)
      .option(MapRDBSourceConfig.TablePathOption, tableName)
      .option(MapRDBSourceConfig.IdFieldPathOption, "_id")
      .option(MapRDBSourceConfig.CreateTableOption, false)
      .option("checkpointLocation", checkpointLocation)
      .option(MapRDBSourceConfig.BulkModeOption, true)
      .option(MapRDBSourceConfig.SampleSizeOption, 1000)
      .start()

    query.awaitTermination()

    // For debug purpose use console output
    //val query = windowedCounts.writeStream.outputMode("complete").format("console").start().awaitTermination
  }
}

