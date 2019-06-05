
name := "assessment"

version := "1.0"

scalaVersion := "2.11.8"



val sparkVersion = "2.3.0"



libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "com.typesafe" % "config" % "1.3.2",
  "org.elasticsearch" % "elasticsearch-hadoop" % "6.4.1"
)


