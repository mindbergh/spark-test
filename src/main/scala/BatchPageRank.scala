import org.apache.spark.SparkContext._
import org.apache.spark.{SparkConf, SparkContext}
import org.rogach.scallop.ScallopConf

/**
 * Created by fangming on 3/20/15.
 */
object BatchPageRank {

  def main(args: Array[String]) {
    object Conf extends ScallopConf(args) {
      version("Batch Page Rank by Ming")
      val source = opt[String]("source", 's', default = Some("corpus/graph"),
        descr = "The source file to read from.")
      val iters = opt[Int]("iters", 't', default = Some(50),
        descr = "The number of iterations")
      val damping = opt[String]("dumping", 'd', default = Some("0.85"),
        descr = "The damping factor.")
    }

    val iters = Conf.iters()
    val source = Conf.source()
    val damping = Conf.damping().toFloat

    val start = System.nanoTime()

    val sparkConf = new SparkConf().setAppName("PageRank").setMaster("local[*]")
    val ctx = new SparkContext(sparkConf)
    val lines = ctx.textFile(source, 1)
    val links = lines.map{ s =>
      val parts = s.split("\\s+")
      (parts(0), parts(1))
    }.distinct().groupByKey().cache()
    var ranks = links.mapValues(v => 1.0)

    for (i <- 1 to iters) {
      val contribs = links.join(ranks).values.flatMap{ case (urls, rank) =>
        val size = urls.size
        urls.map(url => (url, rank / size))
      }
      ranks = contribs.reduceByKey(_ + _).mapValues((1 - damping) + damping * _)
    }

    val output = ranks.collect()
    output.foreach(tup => println(tup._1 + " has rank: " + tup._2 + "."))

    ctx.stop()
    println("Time took: %f s.".format((System.nanoTime() - start) / 1e9))
  }
}
