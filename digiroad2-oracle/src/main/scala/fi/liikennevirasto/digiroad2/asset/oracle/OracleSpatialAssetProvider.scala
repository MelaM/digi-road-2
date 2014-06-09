package fi.liikennevirasto.digiroad2.asset.oracle

import scala.collection.parallel.ForkJoinTaskSupport
import scala.concurrent.forkjoin.ForkJoinPool
import scala.slick.driver.JdbcDriver.backend.Database
import org.joda.time.LocalDate
import org.slf4j.LoggerFactory
import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.mtk.MtkRoadLink
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase.ds
import fi.liikennevirasto.digiroad2.user.{User, Role, UserProvider}
import fi.liikennevirasto.digiroad2.DigiroadEventBus

class OracleSpatialAssetProvider(eventbus: DigiroadEventBus, userProvider: UserProvider) extends AssetProvider {
  val logger = LoggerFactory.getLogger(getClass)

  private def getMunicipalityName(roadLinkId: Long): String = {
    val municipalityId = OracleSpatialAssetDao.getRoadLinkById(roadLinkId).get.municipalityNumber
    OracleSpatialAssetDao.getMunicipalityNameByCode(municipalityId)
  }

  private def userCanModifyMunicipality(municipalityNumber: Int): Boolean = {
    val user = userProvider.getCurrentUser()
    user.configuration.roles.contains(Role.Operator) || user.configuration.authorizedMunicipalities.contains(municipalityNumber)
  }

  private def userCanModifyAsset(assetId: Long): Boolean =
    getAssetById(assetId).flatMap(a => a.municipalityNumber.map(userCanModifyMunicipality)).getOrElse(false)

  private def userCanModifyRoadLink(roadLinkId: Long): Boolean =
    getRoadLinkById(roadLinkId).map(rl => userCanModifyMunicipality(rl.municipalityNumber)).getOrElse(false)

  def getAssetById(assetId: Long): Option[AssetWithProperties] = {
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.getAssetById(assetId)
    }
  }

  def getAssetByExternalId(externalId: Long): Option[AssetWithProperties] = {
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.getAssetByExternalId(externalId)
    }
  }

  def getAssetPositionByExternalId(externalId: Long): Option[(Double, Double)] = {
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.getAssetPositionByExternalId(externalId)
    }
  }

  def getAssets(assetTypeId: Long, user: User, bounds: Option[BoundingRectangle], validFrom: Option[LocalDate], validTo: Option[LocalDate]): Seq[Asset] = {
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.getAssets(assetTypeId, user, bounds, validFrom, validTo)
    }
  }

  private def validatePresenceOf(requiredProperties: Set[String], properties: Seq[SimpleProperty]): Unit = {
    val providedProperties = properties.map { property =>
      property.publicId
    }.toSet
    val missingProperties = requiredProperties -- providedProperties
    if (!missingProperties.isEmpty) {
      throw new IllegalArgumentException("Missing required properties: " + missingProperties.mkString(", "))
    }
  }

  def createAsset(assetTypeId: Long, lon: Double, lat: Double, roadLinkId: Long, bearing: Int, creator: String, properties: Seq[SimpleProperty]): AssetWithProperties = {
    val definedProperties = properties.filterNot( simpleProperty => simpleProperty.values.isEmpty )
    validatePresenceOf(Set(AssetPropertyConfiguration.ValidityDirectionId), definedProperties)
    Database.forDataSource(ds).withDynTransaction {
      if (!userCanModifyRoadLink(roadLinkId)) {
        throw new IllegalArgumentException("User does not have write access to municipality")
      }
      val asset = OracleSpatialAssetDao.createAsset(assetTypeId, lon, lat, roadLinkId, bearing, creator, definedProperties)
      eventbus.publish("asset:saved", (getMunicipalityName(roadLinkId), asset))
      asset
    }
  }

  def updateAsset(assetId: Long, position: Option[Position], properties: Seq[SimpleProperty]): AssetWithProperties = {
    if (!userCanModifyAsset(assetId)) {
      throw new IllegalArgumentException("User does not have write access to municipality")
    }
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.updateAssetLastModified(assetId, userProvider.getCurrentUser().username)
      if (!properties.isEmpty) {
        OracleSpatialAssetDao.updateAssetProperties(assetId, properties)
      }
      position match {
        case None => logger.debug("not updating position")
        case Some(pos) => OracleSpatialAssetDao.updateAssetLocation(id = assetId, lon = pos.lon, lat = pos.lat, roadLinkId = pos.roadLinkId, bearing = pos.bearing)
      }
      val asset = OracleSpatialAssetDao.getAssetById(assetId).get
      eventbus.publish("asset:saved", (getMunicipalityName(asset.roadLinkId), asset))
      asset
    }
  }

  def updateAssetByExternalId(externalId: Long, properties: Seq[SimpleProperty]): AssetWithProperties = {
    // TODO: Share commonalities between updateAssetByExternalId and updateAsset so that database calls are minimized
    val asset = getAssetByExternalId(externalId)
    updateAsset(asset.get.id, None, properties)
  }

  def getEnumeratedPropertyValues(assetTypeId: Long): Seq[EnumeratedPropertyValue] = {
    AssetPropertyConfiguration.commonAssetPropertyEnumeratedValues ++
      Database.forDataSource(ds).withDynTransaction {
        OracleSpatialAssetDao.getEnumeratedPropertyValues(assetTypeId)
      }
  }

  def updateRoadLinks(roadlinks: Seq[MtkRoadLink]): Unit = {
    // TODO: Verify write access?
    val parallerSeq = roadlinks.par
    parallerSeq.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(20))
    parallerSeq.foreach(RoadlinkProvider.updateRoadLink(ds, _))
  }

  def getRoadLinks(user: User, bounds: Option[BoundingRectangle]): Seq[RoadLink] = {
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.getRoadLinks(user, bounds)
    }
  }

  def getRoadLinkById(roadLinkId: Long): Option[RoadLink] = {
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.getRoadLinkById(roadLinkId)
    }
  }

  def getImage(imageId: Long): Array[Byte] = {
    Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.getImage(imageId)
    }
  }

  def availableProperties(assetTypeId: Long): Seq[Property] = {
    (AssetPropertyConfiguration.commonAssetProperties.values.map(_.propertyDescriptor).toSeq ++ Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.availableProperties(assetTypeId)
    }).sortBy(_.propertyUiIndex)
  }

  def getMunicipalities: Seq[Int] = {
    Database.forDataSource(ds).withDynSession {
      OracleSpatialAssetDao.getMunicipalities
    }
  }

  def assetPropertyNames(language: String): Map[String, String] = {
    AssetPropertyConfiguration.assetPropertyNamesByLanguage(language) ++ Database.forDataSource(ds).withDynTransaction {
      OracleSpatialAssetDao.assetPropertyNames(language)
    }
  }

  def getMunicipalityNameByCode(code: Int): String = {
    Database.forDataSource(ds).withDynSession {
      OracleSpatialAssetDao.getMunicipalityNameByCode(code)
    }
  }

  override def getAssetsByMunicipality(municipality: Int) = {
    Database.forDataSource(ds).withDynSession {
      OracleSpatialAssetDao.getAssetsByMunicipality(municipality)
    }
  }
}
