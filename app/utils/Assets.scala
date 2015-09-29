package utils

import java.net.URL
import controllers.CachedAssets.hashedPaths
import play.api.Play
import org.apache.commons.io.IOUtils

object Assets {

  def inlineResource(path: String) = {
    val url = AssetFinder("public/" + hashedPaths.getOrElse(path, path))
    IOUtils.toString(url)
  }

  val svgSprite = inlineResource("images/svg-sprite.svg")
}

object AssetFinder {
  def apply(assetPath: String): URL = {
    Option(Play.classloader(Play.current).getResource(assetPath)).getOrElse {
      throw AssetNotFoundException(assetPath)
    }
  }
}

case class AssetNotFoundException(assetPath: String) extends Exception {
  override val getMessage: String =
    s"Cannot find asset $assetPath. You probably need to run 'grunt compile'."
}

