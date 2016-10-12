package views.fragments

import play.api.mvc.{Cookie, Request}

import scala.util.Random

object ABTest {
  val TestIdCookieName = s"${"gu.subscriptions.test"}.id"

  def testIdFor[A](implicit request: Request[A]): Int = {
    request.cookies.get(TestIdCookieName) map (_.value.toInt) getOrElse Random.nextInt(Int.MaxValue)
  }

  def testIdCookie(id: Int) = Cookie(TestIdCookieName, id.toString, maxAge = Some(604800))

}
