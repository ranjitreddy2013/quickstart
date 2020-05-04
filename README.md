# quickstart
This is a quick start example to run on HCP container platform.

Here are the steps to follow, I'm assuming HCP K8S container platform 5.0 is installed and running. You can find the instructions here for installation of HCP.
http://docs.bluedata.com/50_k8s-getting-started-with-kubernetes

##Here are the steps:

#1 Deploy Spark Operator
```
  git clone https://github.com/mapr/mapr-operators
  cd mapr-operators
  sh bootstrap/bootstrapinstall.sh
  ```
  
#2 Clone this project 
```
  git clone https://github.com/ranjitreddy2013/quickstart
  ```
  
#3 Build the project
```
  cd quickstart
  mvn clean install
  ```
  
#4. Setup a MapR cluster. You can find steps here for installation of MapR cluster. Note the cluster name, this will be used in creating a cspace in the next step. Copy the jar created in step#3 to maprfs:/tmp/. This will be references in the deployment of Spark example.
   https://mapr.com/docs/61/install.html

#5. Create cspace. Take the cluster name from step #4 and update the file cr-cspace-full-vanilla.yaml in mapr-operators/cspaces folder.
```
   [root@psnode188 cspaces]# cat cr-cspace-full-vanilla.yaml
apiVersion: mke.hpe.com/v1
kind: CSpace
metadata:
  name: mycspace2
spec:
  clusterName: my.cluster.com
  clusterType: external
  systemUserSecret: system-user-secret
  baseImageTag: "201912180140"
  imageRegistry: gcr.io/mapr-hpe
  imagePullSecret: mapr-imagepull-secrets
  logLocation: /var/log/mapr/externalcspace
  coreLocation: /var/log/mapr/externalcspace/cores
  podinfoLocation: /var/log/mapr/podinfo
  externalClusterInfo:
    environmentType: vanilla
    dnsdomain: cluster.local
    useSSSD: false
    externalUserSecret: mapr-user-secrets
    externalConfigMap: mapr-external-cm
    externalHiveSiteConfigMap: mapr-hivesite-cm
    externalServerSecret: mapr-server-secrets
    externalClientSecret: mapr-client-secrets
  quotas:
    resourcequotas:
      cpu: "50000m"
      memory: 2000Gi
  debugging:
    logLevel: INFO
    preserveFailedPods: true
  cspaceservices:
    terminal:
      count: 1
      image: cspaceterminal-6.1.0:201912180140
      sshPort: 7777
      requestcpu: "2000m"
      requestmemory: 8Gi
      logLevel: INFO
    hivemetastore:
      count: 1
      image: hivemeta-2.3:201912180140
      requestcpu: "2000m"
      requestmemory: 8Gi
      logLevel: INFO
    sparkhs:
      count: 1
      image: spark-hs-2.4.4:201912180140
      requestcpu: "2000m"
      requestmemory: 8Gi
      logLevel: INFO
  cspaceCustomizationFiles:
    sssdSecret: sssd-secrets
    ldapClientConfig: ldapclient-cm
    sparkHSConfig: sparkhistory-cm
    hiveMetastoreConfig: hivemetastore-cm
    role: cspace-role
    terminalrole: cspace-terminal-role
    userrole: cspace-user-role
  userList:
    - user1
    - user2
  groupList:
    - group1
    - group2
[root@psnode188 cspaces]# kubectl apply -f cr-cspace-full-vanilla.yaml
```

#6.  Navigate to mapr-operators/examples/spark directory
  create spark-quickstart.yaml  for the quickstart example as shown below. This is an example taken from mapr-spark-wc.yaml and updated to this example. Here update namespace, serviceAccount, mainClass, mainApplicationFile, 
  ```
[root@psnode188 spark]# cat spark-quickstart.yaml 

apiVersion: "sparkoperator.k8s.io/v1beta2"
kind: SparkApplication
metadata:
  name: spark-quickstart
  namespace: mycspace2 # example - externalcspace
spec:
  sparkConf:
    spark.mapr.user.secret: mapr-user-secret-3301297619 # example - mapr-user-secret-123456
    spark.eventLog.enabled: "true"
    spark.eventLog.dir: "maprfs:///apps/spark/mycspace2" # example - maprfs:///apps/spark/externalcspace
  type: Java
  sparkVersion: 2.4.4
  mode: cluster
  image: gcr.io/mapr-hpe/spark-2.4.4:201912180140
  imagePullPolicy: Always
  mainClass: com.hpe.qss.SecurityLogAnalytics
  mainApplicationFile: "maprfs:///tmp/quickstart-1.0-SNAPSHOT-jar-with-dependencies.jar"
  restartPolicy:
    type: Never
  arguments:
  - maprfs:///tmp/passwd # example - maprfs:///tmp/input.txt
  imagePullSecrets:
  - mapr-imagepull-secrets
  driver:
    cores: 1
    coreLimit: "1000m"
    memory: "512m"
    labels:
      version: 2.4.4
    serviceAccount: mapr-mycspace2-cspace-sa # example - mapr-mycspace-cspace-sa
  executor:
    cores: 1
    instances: 2
    memory: "512m"
    labels:
      version: 2.4.4

[root@psnode188 spark]# kubectl apply -f spark-quickstart.yaml 
[root@psnode188 spark]# kubectl get pods -n mycspace2
NAME                              READY   STATUS    RESTARTS   AGE
      cspaceterminal-78d5584798-xgfgc   1/1     Running   0          18d
      hivemeta-6875558dd9-k65cb         1/1     Running   0          18d
      spark-quickstart-driver           1/1     Running   0          16s
      sparkhs-79d5674974-jmr8w          1/1     Running   0          18d
[root@psnode188 spark]# 
```
  
