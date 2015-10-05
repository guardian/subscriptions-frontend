package utils

import org.scalatest.FreeSpec
import StringUtils._

class StringUtilsSpec extends FreeSpec {
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
