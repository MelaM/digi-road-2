package fi.liikennevirasto.digiroad2.authentication

import javax.servlet.http.HttpServletRequest
import fi.liikennevirasto.digiroad2.user.{User, UserProvider}

trait RequestHeaderAuthentication extends Authentication {
  val OamRemoteUserHeader = "OAM_REMOTE_USER"

  def authenticate(request: HttpServletRequest)(implicit userProvider: UserProvider): Option[User] = {
    val remoteUser = request.getHeader(OamRemoteUserHeader)
    if (remoteUser != null) {
      userProvider.getUser(remoteUser)
    } else {
      None
    }
  }
}