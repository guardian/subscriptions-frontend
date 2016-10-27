package views.fragments

import play.api.mvc.{Cookie, Request}

import scala.util.Random

object ABTest {
  val TestIdCookieName = "gu.subscriptions.test.id"

  def testIdFor[A](implicit request: Request[A]): Int = {

    request.getQueryString("stripe").flatMap((str) => if (str == "checkout") {Some(2)} else {Some(1)}).
      getOrElse(((request.cookies.get(TestIdCookieName) map (_.value.toInt)).
        getOrElse(Random.nextInt(2))))
  }

  def testIdCookie(id: Int) = Cookie(TestIdCookieName, id.toString, maxAge = Some(604800))

}
