package utils

import com.gu.identity.model.{PrivateFields, PublicFields, StatusFields, User => IdUser}

object TestIdUser {

  val testUser = IdUser(
    id = "test-user",
    primaryEmailAddress = "test-user@example.com",
    publicFields = PublicFields(None),
    privateFields = PrivateFields(),
    statusFields = StatusFields()
  )
}
