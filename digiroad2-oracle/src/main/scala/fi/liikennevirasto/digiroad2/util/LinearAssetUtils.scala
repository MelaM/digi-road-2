package fi.liikennevirasto.digiroad2.util

import fi.liikennevirasto.digiroad2.ChangeInfo
import fi.liikennevirasto.digiroad2.asset.{LinkGeomSource, SideCode, TrafficDirection}
import fi.liikennevirasto.digiroad2.linearasset.{LinearAsset, PersistedLinearAsset, PieceWiseLinearAsset, RoadLink}

object LinearAssetUtils {
  /**
    * Return true if the vvh time stamp is older than change time stamp
    * and asset may need projecting
    * @param asset SpeedLimit under consideration
    * @param change Change information
    * @return true if speed limit may be outdated
    */
  def newChangeInfoDetected(asset : LinearAsset, change: Seq[ChangeInfo]) = {
    change.map(c => (c.oldId.getOrElse(0), c.newId.getOrElse(0), c.vvhTimeStamp)).exists {
      case (oldId: Long, newId: Long, vvhTimeStamp: Long) => (oldId == asset.linkId || newId == asset.linkId) &&
        vvhTimeStamp > asset.vvhTimeStamp
      case _ => false
    }
  }

  def newChangeInfoDetected(a: PersistedLinearAsset, changes: Seq[ChangeInfo]): Boolean = {
    newChangeInfoDetected(persistedLinearAssetToLinearAsset(a), changes)
  }

  /**
    * Returns true if there are new change informations for roadlink assets.
    * Comparing if the assets vvh time stamp is older than the change time stamp
    * @param roadLink Roadlink under consideration
    * @param changeInfo Change information
    * @param assets Linear assets
    * @return true if there are new change informations for roadlink assets
    */
  def isNewProjection(roadLink: RoadLink, changeInfo: Seq[ChangeInfo], assets: Seq[LinearAsset]) = {
    changeInfo.exists(_.newId == roadLink.linkId) &&
      assets.exists(asset => (asset.linkId == roadLink.linkId) &&
        (asset.vvhTimeStamp < changeInfo.filter(_.newId == roadLink.linkId).maxBy(_.vvhTimeStamp).vvhTimeStamp))
  }

  /* Filter to only those Ids that are no longer present on map and not referred to in change information
     Used by LinearAssetService and SpeedLimitService
   */
  def deletedRoadLinkIds(change: Seq[ChangeInfo], current: Seq[RoadLink]): Seq[Long] = {
    change.filter(_.oldId.nonEmpty).flatMap(_.oldId).filterNot(id => current.exists(rl => rl.linkId == id)).
      filterNot(id => change.exists(ci => ci.newId.getOrElse(0) == id))
  }

  private def persistedLinearAssetToLinearAsset(persisted: PersistedLinearAsset) = {
    PieceWiseLinearAsset(id = persisted.id, linkId = persisted.linkId, sideCode = SideCode.apply(persisted.sideCode),
      value = persisted.value,
      geometry = Seq(), expired = persisted.expired, startMeasure = persisted.startMeasure, endMeasure = persisted.endMeasure,
      endpoints = Set(), modifiedBy = persisted.modifiedBy, modifiedDateTime = persisted.modifiedDateTime, createdBy =
        persisted.createdBy, createdDateTime = persisted.createdDateTime, typeId = persisted.typeId, trafficDirection =
        TrafficDirection.UnknownDirection, vvhTimeStamp = persisted.vvhTimeStamp, geomModifiedDate = persisted.geomModifiedDate, linkSource = persisted.linkSource)
  }
}
