package model

import services.{UserId, IdentityToken, GuestUser}

case class GuestAccountData(password: String, id: String, token: String) {
  def guestUser = GuestUser(UserId(id), IdentityToken(token))
}
