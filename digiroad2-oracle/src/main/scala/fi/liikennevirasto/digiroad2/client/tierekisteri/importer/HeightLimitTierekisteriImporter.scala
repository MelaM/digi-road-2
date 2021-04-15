package fi.liikennevirasto.digiroad2.client.tierekisteri.importer

import fi.liikennevirasto.digiroad2.GeometryUtils
import fi.liikennevirasto.digiroad2.asset.{SideCode, TrHeightLimit}
import fi.liikennevirasto.digiroad2.client.tierekisteri.TierekisteriHeightLimitAssetClient
import fi.liikennevirasto.digiroad2.client.vvh.{VVHClient, VVHRoadlink}
import fi.liikennevirasto.digiroad2.dao.pointasset.OracleHeightLimitDao
import fi.liikennevirasto.digiroad2.dao.{RoadAddress => ViiteRoadAddress}
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.service.pointasset.IncomingHeightLimit
import fi.liikennevirasto.digiroad2.util.Digiroad2Properties
import org.apache.http.impl.client.HttpClientBuilder

class HeightLimitTierekisteriImporter extends PointAssetTierekisteriImporterOperations {

  override def typeId: Int = TrHeightLimit.typeId
  override def assetName = "heightLimits"
  override type TierekisteriClientType = TierekisteriHeightLimitAssetClient
  override def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)
  override def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)

  override val tierekisteriClient = new TierekisteriHeightLimitAssetClient(getProperty(Digiroad2Properties.tierekisteriRestApiEndPoint),
    getProperty("digiroad2.tierekisteri.enabled").toBoolean,
    HttpClientBuilder.create().build())

  protected override def createPointAsset(roadAddress: ViiteRoadAddress, vvhRoadlink: VVHRoadlink, mValue: Double, trAssetData: TierekisteriAssetData): Unit = {
    GeometryUtils.calculatePointFromLinearReference(vvhRoadlink.geometry, mValue).map{
      point =>
        val widthLimit = IncomingHeightLimit(point.x, point.y, vvhRoadlink.linkId, trAssetData.height,
          SideCode.BothDirections.value, Some(GeometryUtils.calculateBearing(vvhRoadlink.geometry)))
        OracleHeightLimitDao.create(widthLimit, mValue, vvhRoadlink.municipalityCode, s"batch_process_$assetName",
          VVHClient.createVVHTimeStamp(), vvhRoadlink.linkSource)
    }
  }
}








