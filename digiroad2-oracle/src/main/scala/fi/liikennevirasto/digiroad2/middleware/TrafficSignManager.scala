package fi.liikennevirasto.digiroad2.middleware

import java.sql.SQLIntegrityConstraintViolationException

import fi.liikennevirasto.digiroad2.service.pointasset.TrafficSignInfo
import fi.liikennevirasto.digiroad2._
import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.dao.linearasset.OracleLinearAssetDao
import fi.liikennevirasto.digiroad2.dao.pointasset.PersistedTrafficSign
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.service.RoadLinkService
import fi.liikennevirasto.digiroad2.service.linearasset.ManoeuvreService

object TrafficSignManager {
  val manoeuvreRelatedSigns : Seq[TrafficSignType] =  Seq(NoLeftTurn, NoRightTurn, NoUTurn)
  def belongsToManoeuvre(intValue: Int) : Boolean = {
    manoeuvreRelatedSigns.contains(TrafficSignType.applyOTHValue(intValue))
  }

  val prohibitionRelatedSigns : Seq[TrafficSignType] = Seq(ClosedToAllVehicles,  NoPowerDrivenVehicles,  NoLorriesAndVans,  NoVehicleCombinations, NoAgriculturalVehicles,
    NoMotorCycles,  NoMotorSledges, NoBuses,  NoMopeds,  NoCyclesOrMopeds,  NoPedestrians,  NoPedestriansCyclesMopeds,  NoRidersOnHorseback)
  def belongsToProhibition(intValue: Int) : Boolean = {
    prohibitionRelatedSigns.contains(TrafficSignType.applyOTHValue(intValue))
  }

  val hazmatRelatedSigns : Seq[TrafficSignType] = Seq(NoVehiclesWithDangerGoods)
  def belongsToHazmat(intValue: Int) : Boolean = {
    hazmatRelatedSigns.contains(TrafficSignType.applyOTHValue(intValue))
  }
}

case class TrafficSignManager(manoeuvreService: ManoeuvreService, roadLinkService: RoadLinkService) {
  def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)

  lazy val linearAssetDao: OracleLinearAssetDao = {
    new OracleLinearAssetDao(roadLinkService.vvhClient, roadLinkService)
  }

    def createAssets(trafficSignInfo: TrafficSignInfo, newTransaction: Boolean = true): Unit = {
    trafficSignInfo match {
      case trSign if TrafficSignManager.belongsToManoeuvre(trSign.signType) =>
        manoeuvreService.createBasedOnTrafficSign(trSign, newTransaction)

      case trSign if TrafficSignManager.belongsToProhibition(trSign.signType) =>
        insertTrafficSignToProcess(trSign.id, Prohibition)

      case trSign if TrafficSignManager.belongsToHazmat(trSign.signType) =>
        insertTrafficSignToProcess(trSign.id, HazmatTransportProhibition)

      case _ => None
    }
  }

  def deleteAssets(trafficSign: Seq[PersistedTrafficSign]): Unit = {
    val username = Some("automatic_trafficSign_deleted")

    trafficSign.foreach { trSign =>
      val trafficSignType = trSign.propertyData.find(p => p.publicId == "trafficSigns_type").get.values.map(_.asInstanceOf[TextPropertyValue]).head.propertyValue.toInt

      trafficSignType match {
        case signType if TrafficSignManager.belongsToManoeuvre(signType) =>
          manoeuvreService.deleteManoeuvreFromSign(manoeuvreService.withIds(Set(trSign.id)), username)

        case signType  if TrafficSignManager.belongsToProhibition(signType) =>
            insertTrafficSignToProcess(trSign.id, Prohibition)

        case signType if TrafficSignManager.belongsToHazmat(signType) =>
            insertTrafficSignToProcess(trSign.id, HazmatTransportProhibition)

      }
    }
  }

  def insertTrafficSignToProcess(id: Long, assetInfo: AssetTypeInfo) : Unit = {
    try {
      withDynTransaction {
        linearAssetDao.insertTrafficSignsToProcess(id, assetInfo.typeId)
      }
    } catch {
      case ex: SQLIntegrityConstraintViolationException => print("try insert duplicate key")
      case e: Exception => print("SQL Exception ")
        throw new RuntimeException("SQL exception " + e.getMessage)
    }
  }
}