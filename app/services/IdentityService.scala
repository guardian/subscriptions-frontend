package services

import configuration.Config

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object IdentityService {

  def findUserIdForEmail(email:String) = IdentityApiClient.userLookupByEmail(email).map(_.id)
}

case class IdUser(id:String)

object IdentityApiClient {

  val identityEndpoint = Config.Identity.root

  def userLookupByEmail(email:String):Future[IdUser] = ???
}
