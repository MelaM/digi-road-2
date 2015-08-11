package fi.liikennevirasto.digiroad2.asset.oracle

import fi.liikennevirasto.digiroad2.{EventBusMassTransitStop, DigiroadEventBus, Point, RoadLinkService}
import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase.ds
import fi.liikennevirasto.digiroad2.user.{User, UserProvider}
import org.apache.commons.lang3.StringUtils.isBlank
import org.joda.time.LocalDate
import org.slf4j.LoggerFactory

import scala.slick.driver.JdbcDriver.backend.Database

trait DatabaseTransaction {
  def withDynTransaction[T](f: => T): T
}
object DefaultDatabaseTransaction extends DatabaseTransaction {
  override def withDynTransaction[T](f: => T): T = Database.forDataSource(ds).withDynTransaction(f)
}

// FIXME:
// - move common asset functionality to asset service
class OracleSpatialAssetProvider(eventbus: DigiroadEventBus, userProvider: UserProvider, databaseTransaction: DatabaseTransaction = DefaultDatabaseTransaction) extends AssetProvider {
  val logger = LoggerFactory.getLogger(getClass)

  private def userCanModifyMunicipality(municipalityNumber: Int): Boolean = {
    val user = userProvider.getCurrentUser()
    user.isOperator() || user.isAuthorizedToWrite(municipalityNumber)
  }

  private def userCanModifyAsset(asset: AssetWithProperties): Boolean =
    userCanModifyMunicipality(asset.municipalityNumber)

  def getAssetById(assetId: Long): Option[AssetWithProperties] = {
    databaseTransaction.withDynTransaction {
      OracleSpatialAssetDao.getAssetById(assetId)
    }
  }

  private def eventBusMassTransitStop(asset: AssetWithProperties, municipalityName: String): EventBusMassTransitStop = {
    EventBusMassTransitStop(municipalityNumber = asset.municipalityNumber, municipalityName = municipalityName,
      nationalId = asset.nationalId, lon = asset.lon, lat = asset.lat, bearing = asset.bearing, validityDirection = asset.validityDirection,
      created = asset.created, modified = asset.modified, propertyData = asset.propertyData)
  }

  def updateAsset(assetId: Long, position: Option[Position], properties: Seq[SimpleProperty]): AssetWithProperties = {
    databaseTransaction.withDynTransaction {
      val asset = OracleSpatialAssetDao.getAssetById(assetId).get
      if (!userCanModifyAsset(asset)) { throw new IllegalArgumentException("User does not have write access to municipality") }
      val updatedAsset = OracleSpatialAssetDao.updateAsset(assetId, position, userProvider.getCurrentUser().username, properties)
      val municipalityName = OracleSpatialAssetDao.getMunicipalityNameByCode(updatedAsset.municipalityNumber)
      eventbus.publish("asset:saved", eventBusMassTransitStop(updatedAsset, municipalityName))
      updatedAsset
    }
  }

  def getEnumeratedPropertyValues(assetTypeId: Long): Seq[EnumeratedPropertyValue] = {
    AssetPropertyConfiguration.commonAssetPropertyEnumeratedValues ++
      databaseTransaction.withDynTransaction {
        OracleSpatialAssetDao.getEnumeratedPropertyValues(assetTypeId)
      }
  }

  def availableProperties(assetTypeId: Long): Seq[Property] = {
    (AssetPropertyConfiguration.commonAssetProperties.values.map(_.propertyDescriptor).toSeq ++ databaseTransaction.withDynTransaction {
      OracleSpatialAssetDao.availableProperties(assetTypeId)
    }).sortBy(_.propertyUiIndex)
  }

  def getMunicipalities: Seq[Int] = {
    Database.forDataSource(ds).withDynSession {
      OracleSpatialAssetDao.getMunicipalities
    }
  }

  def assetPropertyNames(language: String): Map[String, String] = {
    AssetPropertyConfiguration.assetPropertyNamesByLanguage(language) ++ databaseTransaction.withDynTransaction {
      OracleSpatialAssetDao.assetPropertyNames(language)
    }
  }
}
