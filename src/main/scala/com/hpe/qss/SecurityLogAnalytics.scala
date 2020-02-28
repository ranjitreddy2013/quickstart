package com.hpe.qss

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._


object SecurityLogAnalytics {
  def main(args: Array[String]) = {

    val spark: SparkSession = SparkSession.builder()
      .appName("Audit Log stream")
      .getOrCreate()


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
      .option("subscribePattern", "/tmp/stream:audit_log")
      .load()
      .select($"value" cast "string" as "json")
      .select(from_json($"json", schema) as "data")
      .select("data.received_at", "data.auth_status",
        "data.audit_sshd_ip", "data.msg", "data.received_from")

    val filteredLines = lines.filter($"audit_sshd_ip".isNotNull) //filter out rows that don't have source IP

    val windowedCounts = filteredLines.groupBy(
      window($"received_at", s"10 seconds", s"5 seconds"),
      $"audit_sshd_ip")
      .count()
      .orderBy("window")

    val query = windowedCounts.writeStream.outputMode("complete").format("console").start().awaitTermination
  }
}

