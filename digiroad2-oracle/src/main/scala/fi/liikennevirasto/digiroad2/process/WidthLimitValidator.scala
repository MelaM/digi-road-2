package fi.liikennevirasto.digiroad2.process

import fi.liikennevirasto.digiroad2.asset.{AssetTypeInfo, PropertyValue, WidthLimit}
import fi.liikennevirasto.digiroad2.dao.pointasset.PersistedTrafficSign
import fi.liikennevirasto.digiroad2.linearasset.PersistedLinearAsset
import fi.liikennevirasto.digiroad2.service.pointasset.TrafficSignType

class WidthLimitValidator extends MassLimitationValidator{
  override def assetTypeInfo: AssetTypeInfo = WidthLimit
  override def assetName: String = "widthLimit"
  override def assetType: Int = WidthLimit.typeId
  override val allowedTrafficSign: Set[TrafficSignType] = Set(TrafficSignType.NoWidthExceeding, TrafficSignType.FreeWidth)

  override def comparingAssetAndTrafficValue(asset: PersistedLinearAsset, trafficSign: PersistedTrafficSign): Boolean = {
    val assetValue = getAssetValue(asset)
    TrafficSignType.apply(getTrafficSignsProperties(trafficSign, "trafficSigns_type").get.propertyValue.toInt) match {
      case TrafficSignType.NoWidthExceeding =>
        getTrafficSignsProperties(trafficSign, "trafficSigns_value").getOrElse(PropertyValue("")).propertyValue == getAssetValue(asset)
      case TrafficSignType.FreeWidth =>
        getTrafficSignsProperties(trafficSign, "trafficSigns_info").getOrElse(PropertyValue("")).propertyValue == getAssetValue(asset)
      case _ => throw new NumberFormatException(s"Not supported trafficSign on ${assetTypeInfo.label} asset")
    }
  }

}
