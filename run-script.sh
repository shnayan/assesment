#!/bin/bash

ls -la
echo "Running sbt assembly"
sbt assembly
echo "Running docker-compose"
docker-compose up -d 

container=`docker ps | awk ' /spark-master-1/ {print $1}'`

sleep 1
echo "container ID : " $container


docker cp target/scala-2.11/*jar  $container:/home

docker cp src/main/resources/ $container:/home

echo "Running Spark Job to Push data Elastic Index"

docker exec spark-master-1 /spark/bin/spark-submit  --master local --deploy-mode client  home/assessment-assembly-1.0.jar --verbose --conf "spark.driver.extraJavaOptions=-Dconfig.file=/home/resources/application.conf" --conf "spark.executor.extraJavaOptions=-Dconfig.file=home/resources/application.conf