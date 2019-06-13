


name := "assessment"
version := "1.0"
scalaVersion := "2.11.8"



val sparkVersion = "2.3.0"

/*

 managed dependencies automatically downloaded from the repositories

 */

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "com.typesafe" % "config" % "1.3.2",
  "org.elasticsearch" % "elasticsearch-hadoop" % "6.4.1"
)

/*

defining main class for the project

 */

mainClass in (Compile, run) := Some("com.test.assessment")

/*
   mergeStrategy helps in building JAR file by avoiding deduplication from different file content
 */

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}