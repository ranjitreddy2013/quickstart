/opt/mapr/spark/spark-2.4.4/bin/spark-submit --master yarn --name SecurityAnalytics --deploy-mode cluster \
  --num-executors 3 \
  --executor-memory  4G \
  --driver-memory 2G \
  --driver-cores 2     \
  --executor-cores 4  \
  --conf spark.executor.memoryOverhead=6000m \
  --conf spark.sql.shuffle.partitions=24 \
  --class com.hpe.qss.Main quickstart-1.0-SNAPSHOT.jar