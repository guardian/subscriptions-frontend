package utils

import StringUtils._
import org.scalatest.freespec.AnyFreeSpec

class StringUtilsSpec extends AnyFreeSpec {
  "slugify" - {
    "normalize accented characters" in {
      assert(slugify("ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜÝß") == "aaaaaaceeeeiiiinooooouuuuy")
      assert(slugify("àáâãäåçèéêëìíîïñòóôõöùúûüýÿ") == "aaaaaaceeeeiiiinooooouuuuyy")
    }

    "remove symbols" in {
      assert(slugify("Something & something else") == "something-something-else")
      assert(slugify("****£20 /|/|/|/| @blah ^^^") == "20-blah")
    }

    "never have multiple hyphens consecutively" in {
      assert(slugify("blah  +  blah") == "blah-blah")
    }
  }
}
