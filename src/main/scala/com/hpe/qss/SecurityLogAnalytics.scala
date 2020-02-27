package com.hpe.qss

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import sys.process._
import com.mapr.fs.MapRFileSystem
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem



object SecurityLogAnalytics {
  def main(args: Array[String]) = {

    val spark: SparkSession = SparkSession.builder()
      .appName("Audit Log stream")
      .getOrCreate()

    import spark.implicits._

    val schema =
      StructType(
        StructField("received_at", StructType(StructField("$date", TimestampType, false) :: Nil), false)
          :: StructField("auth_status", StringType, true)
          :: StructField("audit_sshd_ip", StringType, true)
          :: StructField("msg", StringType, true)
          :: StructField("received_from", StringType, true)
          :: Nil
      )



    val lines = spark.readStream.format("kafka").option("kafka.bootstrap.servers", "localhost")
      .option("subscribePattern", "/tmp/stream:audit_log")
      .load()
      .select($"value" cast "string" as "json").select(from_json($"json", schema) as "data")
      .select("data.received_at.$date", "data.auth_status", "data.audit_sshd_ip",
        "data.msg", "data.received_from")


    val query = lines.writeStream.format("console").start().awaitTermination
  }
}