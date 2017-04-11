package org.apache.spark.ml.feature

import com.typesafe.config._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

import org.apache.spark.ml.linalg.{DenseVector, SparseVector, Vector, Vectors}

//import org.apache.spark.ml.feature.{HashingTF, IDF, RegexTokenizer, Tokenizer, NGram, StopWordsRemover, MinHashLSH}

object MinHashLSHExample {

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

    val bills = input.repartition(400,col("primary_key")).cache()
    bills.explain

    val tokenizer = new RegexTokenizer().setInputCol("content").setOutputCol("words").setPattern("\\W")
    val tokenized = tokenizer.transform(bills)

    //remove stopwords 
    val remover = new StopWordsRemover().setInputCol("words").setOutputCol("filtered")
    val removed = remover.transform(tokenized) //.select(col("primary_key"),col("content"),col("docversion"),col("docid"),col("state"),col("year"),col("filtered"))

    //ngrams
    val ngramer = new NGram().setN(nGramGranularity).setInputCol("filtered").setOutputCol("ngram")
    val ngramed = ngramer.transform(removed)

    //hashing
    val hasher = new HashingTF().setInputCol("ngram").setOutputCol("keys").setNumFeatures(numTextFeatures)
    val hashed = hasher.transform(ngramed).select("keys","primary_key","state")

    //min hash signatures
    val mh = new MinHashLSH().setNumHashTables(20)
      .setInputCol("keys")
      .setOutputCol("hashes")
      .setSeed(12345)


    //calculate distinct state pairs only
    val dfA = hashed.filter(col("state") === 21)
    val dfB = hashed.filter(col("state") === 8)
    //flat schema
    dfB.printSchema

    val model = mh.fit(hashed)
    val threshold = 1.0
    var results = model.approxSimilarityJoin(dfA, dfB, threshold)

    //Schema is a struct
    results.printSchema

    //select statement on this struct 
    results = results.select(col("datasetA.primary_key").alias("pk1"),col("datasetB.primary_key").alias("pk2"),col("distCol").as("distance"))
    results.write.parquet("/user/alexeys/Meetup2017/lsh_output")

    results.show()

    //also prepare and save the baseline here?


    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0)/1000000000 + "s")
 
    spark.stop()
  }
}
