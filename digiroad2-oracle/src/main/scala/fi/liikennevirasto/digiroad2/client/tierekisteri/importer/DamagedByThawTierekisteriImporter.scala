package fi.liikennevirasto.digiroad2.client.tierekisteri.importer

import fi.liikennevirasto.digiroad2.asset.{DamagedByThaw, SideCode}
import fi.liikennevirasto.digiroad2.client.tierekisteri.TierekisteriDamagedByThawAssetClient
import fi.liikennevirasto.digiroad2.client.vvh.VVHRoadlink
import fi.liikennevirasto.digiroad2.dao.{RoadAddressTEMP, RoadAddress => ViiteRoadAddress}
import fi.liikennevirasto.digiroad2.linearasset.RoadLinkLike
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.service.linearasset.{LinearAssetTypes, Measures}
import org.apache.http.impl.client.HttpClientBuilder

class DamagedByThawTierekisteriImporter extends LinearAssetTierekisteriImporterOperations {

  override def typeId: Int = DamagedByThaw.typeId
  override def assetName = "damagedByThaw"
  override type TierekisteriClientType = TierekisteriDamagedByThawAssetClient
  override def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)
  override def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)

  override val tierekisteriClient = new TierekisteriDamagedByThawAssetClient(getProperty("digiroad2.tierekisteriRestApiEndPoint"),
    getProperty("digiroad2.tierekisteri.enabled").toBoolean,
    HttpClientBuilder.create().build())

  override protected def createLinearAssetVKM(vvhRoadlink: RoadLinkLike, roadAddress: RoadAddressTEMP, section: AddressSection, measures: Measures, trAssetData: TierekisteriAssetData): Unit = {
    ???
  }

  override protected def createLinearAsset(vvhRoadlink: RoadLinkLike, roadAddress: ViiteRoadAddress, section: AddressSection, measures: Measures, trAssetData: TierekisteriAssetData): Unit = {
    val assetId = linearAssetService.dao.createLinearAsset(typeId, vvhRoadlink.linkId, false, SideCode.BothDirections.value,
      measures, "batch_process_" + assetName, vvhClient.roadLinkData.createVVHTimeStamp(), Some(vvhRoadlink.linkSource.value))

    linearAssetService.dao.insertValue(assetId, LinearAssetTypes.numericValuePropertyId, 1)
    trAssetData.weight match {
      case Some(value) =>
        linearAssetService.dao.insertValue(assetId, LinearAssetTypes.damagedByThawPropertyId, value * 1000) //convert Ton to Kg
      case _ => None
    }
    println(s"Created OTH $assetName assets for ${vvhRoadlink.linkId} from TR data with assetId $assetId")
  }
}
