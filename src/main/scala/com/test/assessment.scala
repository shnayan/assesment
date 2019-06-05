import org.apache.spark.sql.SparkSession
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.functions._
import org.apache.spark.sql.functions.input_file_name
import org.elasticsearch.spark.sql._
/**
  * Created by b0203364 on 6/4/19.
  */
object assessment {

  val config = ConfigFactory.load("application.conf")
  val appName = config.getString("spark.appName")
  val master = config.getString("spark.master")
  val esNode = config.getString("spark.elasticNode")
  val esPort = config.getInt("spark.elasticPort")
  val outcomeDir = config.getString("directory.outcome")
  val streetDir = config.getString("directory.street")


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

    val outcomeD = spark.read.option("header", "true").option("inferSchema", "false").csv(outcomeDir)
    val streetD = spark.read.option("header", "true").option("inferSchema", "false").csv(streetDir)
    def remove_string: String => String = _.replaceAll("-", " ")
    def remove_string_udf = udf(remove_string)

    val outcome=  outcomeD.withColumn("cus_val", regexp_extract(input_file_name, "(.*\\d)\\-(.*)(\\-)", 2)).withColumn("districtName",remove_string_udf($"cus_val"))
                    .select("Outcome type","Crime ID")

    val street=  streetD.withColumn("cus_val", regexp_extract(input_file_name, "(.*\\d)\\-(.*)(\\-)", 2)).withColumn("districtName",remove_string_udf($"cus_val"))
    val joinedDF = outcome.join(street,Seq("Crime ID"),"full").dropDuplicates()
        joinedDF.filter($"Crime ID"==="98096d1a69205691a56b89c1182eadd6aaf15400ea18da134e0023f20aba5cdb").show()
    val finalDF = joinedDF.withColumn("lastOutcome",when($"Outcome type".isNotNull,$"Outcome type")
      .otherwise($"Last outcome category"))
      .select("Crime ID","districtName","Latitude","Longitude","Crime type","lastOutcome")


    val schStr=finalDF.columns.map(x=> x match { case a if a.contains(" ") => { val q=a.split(" ");
            q(0).toLowerCase()+q(1).capitalize}
          case a => a.toLowerCase } )

      val finalData = finalDF.toDF(schStr:_*)

      finalData.saveToEs("assessment/crimeData")


  }

}

