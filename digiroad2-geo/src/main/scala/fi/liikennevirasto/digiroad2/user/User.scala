package fi.liikennevirasto.digiroad2.user

case class Configuration(
    zoom: Option[Long] = None,
    east: Option[Long] = None,
    north: Option[Long] = None,
    municipalityNumber: Option[Int]  = None,
    authorizedMunicipalities: Set[Int] = Set(),
    roles: Set[String] = Set()
)
case class User(id: Long, username: String, configuration: Configuration) {
  def hasWriteAccess() = {
    configuration.roles.map(_.toLowerCase).contains("viewer") == false
  }
}

object Role {
  val Operator = "operator"
  val Administrator = "administrator"
}