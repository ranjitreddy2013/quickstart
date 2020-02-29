/opt/mapr/spark/spark-2.4.4/bin/spark-submit --master yarn --name SecurityLogAnalytics --deploy-mode cluster \
  --num-executors 1 \
  --executor-memory  2G \
  --driver-memory 1G \
  --driver-cores 1     \
  --executor-cores 2  \
  --conf spark.executor.memoryOverhead=1000m \
  --conf spark.sql.shuffle.partitions=24 \
  --class com.hpe.qss.SecurityLogAnalytics quickstart-1.0-SNAPSHOT-jar-with-dependencies.jar
