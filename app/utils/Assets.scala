package utils

import com.typesafe.scalalogging.LazyLogging
import scala.io.Source

object Assets extends LazyLogging {
  def inlineResource(path: String) = (for {
    resourceIs <- Option(getClass.getClassLoader.getResourceAsStream(path))
  } yield Some(Source.fromInputStream(resourceIs).mkString)).getOrElse {
    logger.warn(s"Could not find $path. You may need to run grunt compile")
    None
  }

  val svgSprite = inlineResource("assets/svg-sprite.svg")
}
