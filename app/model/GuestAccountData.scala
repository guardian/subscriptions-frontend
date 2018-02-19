package model

import services.{GuestUser, IdentityToken, UserId}

case class GuestAccountData(password: String, id: String, token: String) {
  def guestUser = GuestUser(UserId(id), IdentityToken(token))
}
