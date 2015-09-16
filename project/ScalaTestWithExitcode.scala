import sbt._

import scala.util.Try

class ScalaTestWithExitCode extends TestResultLogger {
  import sbt.Tests._

  override def run(log: Logger, results: Output, taskName: String): Unit = {
    results.overall match {
      case TestResult.Error | TestResult.Failed => sys.exit(1)
      case _ =>
        val summary = findScalatestSummary(results.summaries)
                        .fold[Map[String, Int]](Map.empty)(parseScalatestSummary)

        if (summary.exists(t => t._1 == "failed" && t._2 > 0)) {
          logWarnings(log, results)
          sys.exit(1)
        }
    }
  }

  private def logWarnings(log: Logger, results: Output) = {
    log.warn("SBT test result doesn't match the test summary provided by ScalaTest:")
    log.warn(s"results.overall: ${results.overall}")
    log.warn(s"results.events: ${results.events.values.toList.head}")
    log.warn(s"results.summaries: ${results.summaries}")
  }

  private def findScalatestSummary(summaries: Iterable[Summary]): Option[String] = for {
    scalatestSummary <- summaries.find(_.name == "ScalaTest")
    lines = scalatestSummary.summaryText.split("\n")
    line <- lines.find(_.contains("Tests:"))
  } yield line

  private def parseScalatestSummary(summary: String): Map[String, Int] = {
    """\[\d\dmTests:""".r.replaceAllIn(summary, "").split(", ")
      .foldLeft(Map.empty[String, Int]) { case (acc, token) =>
        token.trim.split(" ") match {
          case Array(attr, ns) if Try(ns.toInt).isSuccess =>
            acc + (attr -> ns.toInt)
          case _ =>
            acc
        }
    }
  }

}
