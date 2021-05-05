package com.gu.acquisition.model

class SyntheticPageviewId(length: Int, suffix: String) {
  import scala.util.Random
  import java.lang.Long

  private def base36(long: Long): String = Long.toString(long, 36)

  /**
    * Generate a pageview ID using the same logic (barring the suffix) used
    * in tracker-js
    *
    * @see https://github.com/guardian/ophan/blob/master/tracker-js/assets/coffee/ophan/transmit.coffee#L3
    */
  def generate: String = {
    val prefix: String = base36(System.currentTimeMillis)
    val infix: String = Stream.continually(Random.nextInt(36))
      .map(x => base36(x.toLong))
      .take(length - prefix.length - suffix.length)
      .mkString

    s"$prefix$infix$suffix"
  }
}

object SyntheticPageviewId {
  val defaultLength = 20
  val defaultSuffix = "AEP"

  private val defaultInstance = new SyntheticPageviewId(defaultLength, defaultSuffix)

  def generate: String = defaultInstance.generate
}
