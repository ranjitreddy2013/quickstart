package com.hpe.qss

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import sys.process._
import com.mapr.fs.MapRFileSystem
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem



object VolumeMonitoring {
  def main(args: Array[String]) = {

    val spark: SparkSession = SparkSession.builder()
      .appName("Read Audit Log stream")
      .getOrCreate()

    import spark.implicits._

    val schema =
      StructType(
        StructField("timestamp", StructType(StructField("$date", TimestampType, false) :: Nil), false)
          :: StructField("uid", IntegerType, true)
          :: StructField("status", IntegerType, true)
          :: StructField("operation", StringType, true)
          :: StructField("ipAddress", StringType, true)
          :: StructField("nfsServer", StringType, true)
          :: StructField("srcFid", StringType, true)
          :: StructField("volumeId", IntegerType, true)
          :: Nil
      )

    val toUsername = spark.udf.register("toUsername", (x: Int) => {
      ("getent passwd " + x !!).split(":")(0)
    })
    val toVolumeName = spark.udf.register("toVolumeName", (x: Int) => {
      val fs = FileSystem.get(new Configuration()).asInstanceOf[MapRFileSystem]
      //if (x == null) "unknown" else fs.getVolumeName(x)
      if (x == null) "unknown" else "testvolume"
    })

    val toFilePath = spark.udf.register("toFilePath", (x: String) => {
      val fs = FileSystem.get(new Configuration()).asInstanceOf[MapRFileSystem]
      if (x == null) "unknown" else fs.getMountPathFidCached(x)
    })


    val lines = spark.readStream.format("kafka").option("kafka.bootstrap.servers", "localhost")
      .option("subscribePattern", "/var/mapr/auditstream/auditlogstream:^my.cluster.com_fs_.+")
      .load()
      .select($"value" cast "string" as "json").select(from_json($"json", schema) as "data")
      .select("data.timestamp.$date", "data.uid", "data.status",
        "data.operation", "data.ipAddress", "data.nfsServer", "data.srcFid", "data.volumeId")

   // val violations = lines.filter($"status" > 0)

    val violationsExpanded = lines.withColumn("username", toUsername(col("uid"))).withColumn("volumeName", toVolumeName(col("volumeId"))).withColumn("filePath", toFilePath(col("srcFid")))

    val query = violationsExpanded.writeStream.format("console").start().awaitTermination
  }
}