package views.support

import play.twirl.api.Html

object CommaSplit {
def split(input: String):Html ={
  Html(input.split(',').mkString(",</br>"))
}
}
