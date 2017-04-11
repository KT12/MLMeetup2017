import com.typesafe.config._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import org.apache.spark.ml.feature.{HashingTF, IDF, RegexTokenizer, Tokenizer, NGram, StopWordsRemover, MinHashLSH}

import org.apache.spark.ml.{Pipeline, PipelineModel}

//for custom transformer
import org.apache.spark.ml.{UnaryTransformer}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.{ArrayType, DataType, StringType}

object PipelineExample {

  //regular approach - using UDF
  def cleaner_udf = udf((s: String) => s.replaceAll("(\\d|,|:|;|\\?|!)", ""))

  //alternative approach - using a custom Transformer class 
  class Cleaner(override val uid: String)
    extends UnaryTransformer[String, String, Cleaner]  {

    def this() = this(Identifiable.randomUID("textcleaner"))

    def cleaner_f(s: String) : String = {
       s.replaceAll("(\\d|,|:|;|\\?|!)", "")
    }

    override protected def createTransformFunc: String => String = {
      cleaner_f _
    }

    override protected def validateInputType(inputType: DataType): Unit = {
      require(inputType == StringType)
    }

    override protected def outputDataType: DataType = DataTypes.StringType
  }

  def main(args: Array[String]) {

    val spark = SparkSession.builder().appName("MinHashExample")
      .config("spark.shuffle.service.enabled","true")
      .config("spark.shuffle.memoryFraction","0.6")
      .config("spark.sql.codegen.wholeStage", "true")
      .config("spark.driver.maxResultSize", "10g")
      .getOrCreate()

    import spark.implicits._

    val t0 = System.nanoTime()

    val params = ConfigFactory.load("examplePipeline")

    val vv: String = params.getString("examplePipeline.docVersion")
    val input = spark.read.json(params.getString("examplePipeline.inputFile")).filter($"docversion" === vv)
    val nGramGranularity = params.getInt("examplePipeline.nGramGranularity")
    val numTextFeatures = params.getInt("examplePipeline.numTextFeatures")

    var bills = input.repartition(400,col("primary_key")).cache()
    bills.explain

    //clean
    //approach 1: standard UDF
    //bills = bills.withColumn("content",cleaner_udf(col("content"))) 
    val cleaner = new Cleaner().setInputCol("content").setOutputCol("cleaned")
    //val inputCol = cleaner.getInputCol
  
    //tokenize
    val tokenizer = new RegexTokenizer().setInputCol("cleaned").setOutputCol("words").setPattern("\\W")

    //remove stopwords 
    val remover = new StopWordsRemover().setInputCol("words").setOutputCol("filtered")

    //extract ngrams
    val ngram = new NGram().setN(nGramGranularity).setInputCol("filtered").setOutputCol("ngram")

    //hashing
    val hashingTF = new HashingTF().setInputCol("ngram").setOutputCol("keys").setNumFeatures(numTextFeatures)

    //min hash
    val mh = new MinHashLSH().setNumHashTables(20)
      .setInputCol("keys")
      .setOutputCol("hashes")
      .setSeed(12345)


    val pipeline = new Pipeline()
      .setStages(Array(cleaner, tokenizer, remover, ngram, hashingTF, mh))

    // Fit the pipeline to training documents.
    val model = pipeline.fit(bills)

    //save model to disk and load into Pipelinemodel later
    //model.write.overwrite().save("/user/alexeys/Meetup2017/pipeline_model")

    // Make predictions on test documents.
    val results = model.transform(bills)

    //inspect features
    results.select("primary_key","ngram","keys").show()

    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0)/1000000000 + "s")
 
    spark.stop()
  }
}
