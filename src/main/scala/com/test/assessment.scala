package com.test

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{input_file_name, _}
import org.elasticsearch.spark.sql._
/**
  * Created by b0203364 on 6/4/19.
  */
object assessment {

  /*

  Loading configuration from application.conf

   */

  val config = ConfigFactory.load("application.conf")
  val appName = config.getString("spark.appName")
  val master = config.getString("spark.master")
  val esNode = config.getString("spark.elasticNode")
  val esPort = config.getInt("spark.elasticPort")
  val esIndex = config.getInt("spark.esIndex")
  val outcomeDir = config.getString("directory.outcome")
  val streetDir = config.getString("directory.street")

  /*

    Creating spark session using SparkSession builder method with configuration from application.conf

    */

  val spark = SparkSession
    .builder()
    .appName(appName)
    .master(master)
    .config("spark.es.nodes",esNode)
    .config("spark.es.port",esPort)
    .config("es.index.auto.create", "true")
    .getOrCreate()

  import spark.implicits._

  def main(args: Array[String]): Unit = {

    /*
       outcomeD :Dataframe  => reading all the <district>-outcomes.csv
       streetD : Dataframe => reading all the <district>.csv

     */
    val outcomeD = spark.read.option("header", "true").option("inferSchema", "false").csv(outcomeDir)
    val streetD = spark.read.option("header", "true").option("inferSchema", "false").csv(streetDir)

    /*

     Exacting fields from the csv.
     input_file_name() gets the  fileName  and using  regexp_extract() and replaceAll() over fileName to extract districtName as column
     */

    def remove_string: String => String = _.replaceAll("-", " ")
    def remove_string_udf = udf(remove_string)

    val outcome=  outcomeD.withColumn("cus_val", regexp_extract(input_file_name, "(.*\\d)\\-(.*)(\\-)", 2)).withColumn("districtName",remove_string_udf($"cus_val"))
                    .select("Outcome type","Crime ID")

    val street=  streetD.withColumn("cus_val", regexp_extract(input_file_name, "(.*\\d)\\-(.*)(\\-)", 2)).withColumn("districtName",remove_string_udf($"cus_val"))
/*
      joinedDF :DataFrame  => joining outcome :DataFrame  and street :DataFrame to get all the data together.
      taking outcome form outcome :DataFrame which was ask, solve using when condition and
      in otherwise condition

      Optimization
      Full join can be avoided here which may lead to the issue if the dataFrame beacame too large
      Better way to do with => inner join(outcome and street)  on crimeID say d1 :DataFrame
                          d2 :DataFrame right outer join d1 with original <district>.csv file i.e. street :DataFrame
                         d1.union(d2) for desired result.
 */

    val joinedDF = outcome.join(street,Seq("Crime ID"),"full").dropDuplicates()
    val finalDF = joinedDF.withColumn("lastOutcome",when($"Outcome type".isNotNull,$"Outcome type")
      .otherwise($"Last outcome category"))
      .select("Crime ID","districtName","Latitude","Longitude","Crime type","lastOutcome")

    /*

    method to change columns name to camel case
      "Crime ID" => crimeID
      "Crime Type" => crimeType
     */

    val schStr=finalDF.columns.map(x=> x match { case a if a.contains(" ") => { val q=a.split(" ");
            q(0).toLowerCase()+q(1).capitalize}
          case a => a.toLowerCase } )

      val finalData = finalDF.toDF(schStr:_*)
    /*
        sending the finaData :DataFrame to elastic index.
     */

      finalData.saveToEs()


  }

}

