package utils

import org.scalatest.FreeSpec
import StringUtils._

class StringUtilsSpec extends FreeSpec {
  "slugify" - {
    "normalize accented characters" in {
      assert(slugify("ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝß") == "aaaaaaceeeeiiiinooooouuuuy")
      assert(slugify("àáâãäåçèéêëìíîïñòóôõöùúûüýÿ") == "aaaaaaceeeeiiiinooooouuuuyy")
    }

    "normalize accented characters" in {
      assert(slugify("Something & something else") == "something-something-else")
      assert(slugify("àáâãäåçèéêëìíîïñòóôõöùúûüýÿ") == "aaaaaaceeeeiiiinooooouuuuyy")
    }

    "never have multiple hyphens consecutively" in {
      assert(slugify("blah  +  blah") == "blah-blah")
    }
  }
}
