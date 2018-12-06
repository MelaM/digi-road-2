package fi.liikennevirasto.digiroad2.asset

import fi.liikennevirasto.digiroad2.{Point, Vector3d}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.util.Try

sealed trait LinkGeomSource{
  def value: Int
}

//LINKIN LÄHDE (1 = tielinkkien rajapinta, 2 = täydentävien linkkien rajapinta, 3 = suunnitelmalinkkien rajapinta, 4 = jäädytettyjen linkkien rajapinta, 5 = historialinkkien rajapinta)

object LinkGeomSource{
  val values = Set(NormalLinkInterface, ComplimentaryLinkInterface , SuravageLinkInterface, FrozenLinkInterface, HistoryLinkInterface)

  def apply(intValue: Int): LinkGeomSource = values.find(_.value == intValue).getOrElse(Unknown)

  case object NormalLinkInterface extends LinkGeomSource {def value = 1;}
  case object ComplimentaryLinkInterface extends LinkGeomSource {def value = 2;}
  case object SuravageLinkInterface extends LinkGeomSource {def value = 3;}
  case object FrozenLinkInterface extends LinkGeomSource {def value = 4;}
  case object HistoryLinkInterface extends LinkGeomSource {def value = 5;}
  case object Unknown extends LinkGeomSource { def value = 99 }
}

sealed trait ConstructionType {
  def value: Int
}

object ConstructionType{
  val values = Set[ConstructionType](InUse, UnderConstruction, Planned, UnknownConstructionType)

  def apply(intValue: Int): ConstructionType = {
    values.find(_.value == intValue).getOrElse(InUse)
  }

  case object InUse extends ConstructionType { def value = 0 }
  case object UnderConstruction extends ConstructionType { def value = 1 }
  case object Planned extends ConstructionType { def value = 3 }
  case object UnknownConstructionType extends ConstructionType { def value = 99 }
}

sealed trait LinkType
{
  def value: Int
}
object LinkType {
  val values = Set(Motorway, MultipleCarriageway, SingleCarriageway, Freeway, Roundabout, SlipRoad,
                   RestArea, CycleOrPedestrianPath, PedestrianZone, ServiceOrEmergencyRoad, EnclosedTrafficArea,
                   TractorRoad, MotorwayServiceAccess, CableFerry, SpecialTransportWithoutGate, SpecialTransportWithGate, UnknownLinkType)

  def apply(value: Int): LinkType = {
    values.find(_.value == value).getOrElse(UnknownLinkType)
  }
}
case object Motorway extends LinkType { def value = 1 }
case object MultipleCarriageway extends LinkType { def value = 2 }
case object SingleCarriageway extends LinkType { def value = 3 }
case object Freeway extends LinkType { def value = 4 }
case object Roundabout extends LinkType { def value = 5 }
case object SlipRoad extends LinkType { def value = 6 }
case object RestArea extends LinkType { def value = 7 }
case object CycleOrPedestrianPath extends LinkType { def value = 8 }
case object PedestrianZone extends LinkType { def value = 9 }
case object ServiceOrEmergencyRoad extends LinkType { def value = 10 }
case object EnclosedTrafficArea extends LinkType { def value = 11 }
case object TractorRoad extends LinkType { def value = 12 }
case object MotorwayServiceAccess extends LinkType { def value = 13 }
case object SpecialTransportWithoutGate extends LinkType { def value = 14 }
case object SpecialTransportWithGate extends LinkType { def value = 15 }
case object CableFerry extends LinkType { def value = 21 }
case object UnknownLinkType extends LinkType { def value = 99 }

sealed trait AdministrativeClass {
  def value: Int
}
object AdministrativeClass {
  val values = Set(State, Municipality, Private, Unknown)

  def apply(value: Int): AdministrativeClass = {
    values.find(_.value == value).getOrElse(Unknown)
  }

  def apply(stringValue: String): AdministrativeClass = {
    values.find(_.toString == stringValue).getOrElse(Unknown)
  }
}
case object State extends AdministrativeClass { def value = 1 }
case object Municipality extends AdministrativeClass { def value = 2 }
case object Private extends AdministrativeClass { def value = 3 }
case object Unknown extends AdministrativeClass { def value = 99 }



sealed trait InformationSource {
  def value: Int
}

object InformationSource{
  val values = Set(RoadRegistry, MunicipalityMaintenainer, MmlNls, UnknownSource)

  def apply(value: Int): InformationSource = {
    values.find(_.value == value).getOrElse(UnknownSource)
  }
}

//1 = FTA/ Road registry (Liikennevirasto / Tierekisteri)
case object RoadRegistry extends InformationSource { def value = 1 }
//2 = Maintainer (municipality maintainer)
case object MunicipalityMaintenainer extends InformationSource { def value = 2 }
//3 = MML/NLS (Maanmittauslaitos)
case object MmlNls extends InformationSource { def value = 3 }

case object UnknownSource extends InformationSource { def value = 99 }


object FunctionalClass {
  val Unknown: Int = 99
}

sealed trait TrafficDirection {
  def value: Int
  def isOneWay =
    this == TrafficDirection.AgainstDigitizing ||
    this == TrafficDirection.TowardsDigitizing
}
object TrafficDirection {
  val values = Set(BothDirections, AgainstDigitizing, TowardsDigitizing, UnknownDirection)

  def apply(intValue: Int): TrafficDirection = {
    values.find(_.value == intValue).getOrElse(UnknownDirection)
  }

  def apply(optionalValue: Option[Int]): TrafficDirection = {
    optionalValue.map { value => values.find(_.value == value).getOrElse(UnknownDirection) }.getOrElse(UnknownDirection)
  }

  def apply(stringValue: String): TrafficDirection = {
    values.find(_.toString == stringValue).getOrElse(UnknownDirection)
  }

  def toSideCode(trafficDirection: TrafficDirection): SideCode = {
    trafficDirection match {
      case TowardsDigitizing => SideCode.TowardsDigitizing
      case AgainstDigitizing => SideCode.AgainstDigitizing
      case BothDirections => SideCode.BothDirections
      case UnknownDirection => SideCode.Unknown
    }
  }

  case object BothDirections extends TrafficDirection { def value = 2 }
  case object AgainstDigitizing extends TrafficDirection { def value = 3 }
  case object TowardsDigitizing extends TrafficDirection { def value = 4 }
  case object UnknownDirection extends TrafficDirection { def value = 99 }


}

sealed trait SideCode {
  def value: Int
}
object SideCode {
  val values = Set(BothDirections, TowardsDigitizing, AgainstDigitizing, Unknown)

  def apply(intValue: Int): SideCode = {
    values.find(_.value == intValue).getOrElse(Unknown)
  }

  def switch(sideCode: SideCode): SideCode = {
    sideCode match {
      case TowardsDigitizing => AgainstDigitizing
      case AgainstDigitizing => TowardsDigitizing
      case _ => sideCode
    }
  }

  def toTrafficDirection(sideCode: SideCode): TrafficDirection = {
    sideCode match {
      case TowardsDigitizing => TrafficDirection.TowardsDigitizing
      case AgainstDigitizing => TrafficDirection.AgainstDigitizing
      case BothDirections => TrafficDirection.BothDirections
      case Unknown => TrafficDirection.UnknownDirection
    }
  }

  case object BothDirections extends SideCode { def value = 1 }
  case object TowardsDigitizing extends SideCode { def value = 2 }
  case object AgainstDigitizing extends SideCode { def value = 3 }
  case object Unknown extends SideCode { def value = 99 }
}

/**
  * Values for PavementClass types enumeration
  */
sealed trait PavementClass {
  def value: Int
  def typeDescription: String
}
object PavementClass {
  val values = Set(CementConcrete, Cobblestone, HardAsphalt, SoftAsphalt, GravelSurface, GravelWearLayer, OtherCoatings, Unknown)

  def apply(value: Int): PavementClass = {
    values.find(_.value == value).getOrElse(Unknown)
  }

  case object CementConcrete extends PavementClass { def value = 1; def typeDescription = "Cement Concrete";}
  case object Cobblestone extends PavementClass { def value = 2; def typeDescription = "Cobblestone";}
  case object HardAsphalt extends PavementClass { def value = 10; def typeDescription = "Hard Asphalt";}
  case object SoftAsphalt extends PavementClass { def value = 20; def typeDescription = "Soft Asphalt";}
  case object GravelSurface extends PavementClass { def value = 30; def typeDescription = "Gravel Surface";}
  case object GravelWearLayer extends PavementClass { def value = 40; def typeDescription = "Gravel Wear Layer";}
  case object OtherCoatings extends PavementClass { def value = 50; def typeDescription = "Other Coatings";}
  case object Unknown extends PavementClass { def value = 99;  def typeDescription = "Unknown";}
}


sealed trait ServicePointsClass {
  def value: Int
  def isAuthorityData: Boolean
}
object ServicePointsClass {
  val values = Set(Customs, BorderCrossing, RestArea, Airport, FerryTerminal, RailwayStation, ParkingArea, TerminalForLoadingCars,
                  ParkingAreaBusesAndTrucks, ParkingGarage, BusStation, TaxiStation, ElectricCarChargingStation, Unknown)

  def apply(value: Int): Boolean = {
    values.find(_.value == value).getOrElse(Unknown).isAuthorityData
  }

  case object Customs extends ServicePointsClass { def value = 4;  def isAuthorityData = true;}
  case object BorderCrossing extends ServicePointsClass { def value = 5; def isAuthorityData = true;}
  case object RestArea extends ServicePointsClass { def value = 6;  def isAuthorityData = true;}
  case object Airport extends ServicePointsClass { def value = 8;  def isAuthorityData = true;}
  case object FerryTerminal extends ServicePointsClass { def value = 9;  def isAuthorityData = true;}
  case object RailwayStation extends ServicePointsClass { def value = 11;  def isAuthorityData = true;}
  case object ParkingArea extends ServicePointsClass { def value = 12;  def isAuthorityData = true;}
  case object TerminalForLoadingCars extends ServicePointsClass { def value = 13;   def isAuthorityData = true;}
  case object ParkingAreaBusesAndTrucks extends ServicePointsClass { def value = 14;   def isAuthorityData = true;}
  case object ParkingGarage extends ServicePointsClass { def value = 15;   def isAuthorityData = true;}
  case object BusStation extends ServicePointsClass { def value = 16;  def isAuthorityData = true;}
  case object TaxiStation extends ServicePointsClass { def value = 10;  def isAuthorityData = false;}
  case object ElectricCarChargingStation extends ServicePointsClass { def value = 17;  def isAuthorityData = false;}
  case object Unknown extends ServicePointsClass { def value = 99;  def isAuthorityData = true;}
}


trait NationalStop { val nationalId: Long }
trait RoadLinkStop {
  val linkId: Option[Long]
  val mValue: Option[Double]
}
trait TimeStamps {
  val created: Modification
  val modified: Modification
}
trait FloatingAsset {
  val id: Long
  val floating: Boolean
}
case class AssetType(id: Long, assetTypeName: String, geometryType: String)

object Asset {
  val DateTimePropertyFormat = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")
  val DatePropertyFormat = DateTimeFormat.forPattern("dd.MM.yyyy")
  val DateTimePropertyFormatMs = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss,SSS")
}

abstract class AbstractProperty {
  def publicId: String
  def values: Seq[PropertyValue]
}

case class Modification(modificationTime: Option[DateTime], modifier: Option[String])
case class SimpleProperty(publicId: String, values: Seq[PropertyValue]) extends AbstractProperty
case class DynamicProperty(publicId: String, propertyType: String, required: Boolean = false, values: Seq[DynamicPropertyValue])
case class Property(id: Long, publicId: String, propertyType: String, required: Boolean = false, values: Seq[PropertyValue], numCharacterMax: Option[Int] = None) extends AbstractProperty
case class PropertyValue(propertyValue: String, propertyDisplayValue: Option[String] = None, checked: Boolean = false)
case class DynamicPropertyValue(value: Any)
case class ValidityPeriodValue(days: Int, startHour: Int, endHour: Int, startMinute: Int, endMinute: Int, periodType: Option[Int] = None)
case class EnumeratedPropertyValue(propertyId: Long, publicId: String, propertyName: String, propertyType: String, required: Boolean = false, values: Seq[PropertyValue]) extends AbstractProperty
case class Position(lon: Double, lat: Double, linkId: Long, bearing: Option[Int])

object ValidityPeriodValue {
  def fromMap(map: Map[String, Any]): ValidityPeriodValue = {
    ValidityPeriodValue(
        getPropertyValuesByPublicId("days", map),
        getPropertyValuesByPublicId("startHour", map),
        getPropertyValuesByPublicId("endHour", map),
        getPropertyValuesByPublicId("startMinute", map),
        getPropertyValuesByPublicId("endMinute", map),
        getOptionalPropertyValuesByPublicId("periodType", map))
  }

  def getPropertyValuesByPublicId(property: String, mapValue: Map[String, Any]): Int = {
    Try(mapValue(property).asInstanceOf[BigInt].toInt).getOrElse(mapValue(property).asInstanceOf[Int])
  }

  def getOptionalPropertyValuesByPublicId(property: String, mapValue: Map[String, Any]): Option[Int] = {
    mapValue.get(property) match {
      case Some(value) => Try(value.asInstanceOf[Option[BigInt]].map(_.toInt)).getOrElse(value.asInstanceOf[Option[Int]])
      case _ => None
    }
  }

  def toMap(value: ValidityPeriodValue):  Map[String, Any] = {
    Map(
      "days" -> value.days,
      "startHour" -> value.startHour,
      "endHour" -> value.endHour,
      "startMinute" -> value.startMinute,
      "endMinute" -> value.endMinute
    )
  }

  def duration(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int ): Int = {
    val startHourAndMinutes: Double = (startMinute / 60.0) + startHour
    val endHourAndMinutes: Double = (endMinute / 60.0) + endHour

    if (endHourAndMinutes > startHourAndMinutes) {
      Math.ceil(endHourAndMinutes - startHourAndMinutes).toInt
    } else {
      Math.ceil(24 - startHourAndMinutes + endHourAndMinutes).toInt
    }
  }
}

object PropertyTypes {
  val SingleChoice = "single_choice"
  val MultipleChoice = "multiple_choice"
  val Text = "text"
  val LongText = "long_text"
  val ReadOnlyText = "read_only_text"
  val ReadOnlyNumber = "read_only_number"
  val Date = "date"
  val ReadOnly = "read-only"
  val CheckBox = "checkbox"
  val Number = "number"
  val IntegerProp = "integer"
  val TimePeriod = "time_period"
}

object MassTransitStopValidityPeriod {
  val Past = "past"
  val Current = "current"
  val Future = "future"
}

case class BoundingRectangle(leftBottom: Point, rightTop: Point) {
  def diagonal: Vector3d = leftBottom - rightTop
  def area: Double = diagonal.x*diagonal.y
}

sealed trait AssetTypeInfo {
  val typeId: Int
  def geometryType: String
  val label: String
  val layerName: String
}
//TODO change the type to be optional since manoeuvre are stored in a separated table and geometry type can be a type and the label can be a toString override
object AssetTypeInfo {
  val values =  Set(MassTransitStopAsset, SpeedLimitAsset,TotalWeightLimit, TrailerTruckWeightLimit, AxleWeightLimit, BogieWeightLimit,
                    HeightLimit, LengthLimit, WidthLimit, LitRoad, PavedRoad, RoadWidth, DamagedByThaw,
                    NumberOfLanes, MassTransitLane, TrafficVolume, WinterSpeedLimit,
                    Prohibition, PedestrianCrossings, HazmatTransportProhibition, Obstacles,
                    RailwayCrossings, DirectionalTrafficSigns, ServicePoints, EuropeanRoads, ExitNumbers,
                    TrafficLights, MaintenanceRoadAsset, TrafficSigns, Manoeuvres, TrTrailerTruckWeightLimit, TrBogieWeightLimit, TrAxleWeightLimit,TrWeightLimit, TrHeightLimit, TrWidthLimit,
                    CareClass, CarryingCapacity, UnknownAssetTypeId)

  def apply(value: Int): AssetTypeInfo = {
    values.find(_.typeId == value).getOrElse(UnknownAssetTypeId)
  }

  def apply(stringValue: String): AssetTypeInfo = {
    values.find(_.toString == stringValue).getOrElse(UnknownAssetTypeId)
  }
}
case object MassTransitStopAsset extends AssetTypeInfo { val typeId = 10; def geometryType = "point"; val label = "MassTransitStop"; val layerName = "massTransitStop"}
case object SpeedLimitAsset extends AssetTypeInfo { val typeId = 20; def geometryType = "linear"; val label = "SpeedLimit"; val layerName = "speedLimits"}
case object TotalWeightLimit extends AssetTypeInfo { val typeId = 30; def geometryType = "linear"; val label = "TotalWeightLimit" ; val layerName = "totalWeightLimit"}
case object TrailerTruckWeightLimit extends AssetTypeInfo { val typeId = 40; def geometryType = "linear"; val label = "TrailerTruckWeightLimit"; val layerName = "trailerTruckWeightLimit" }
case object AxleWeightLimit extends AssetTypeInfo { val typeId = 50; def geometryType = "linear"; val label = "AxleWeightLimit"; val layerName = "axleWeightLimit" }
case object BogieWeightLimit extends AssetTypeInfo { val typeId = 60; def geometryType = "linear"; val label =  "BogieWeightLimit"; val layerName = "bogieWeightLimit" }
case object HeightLimit extends AssetTypeInfo { val typeId = 70; def geometryType = "linear"; val label = "HeightLimit"; val layerName = "heightLimit" }
case object LengthLimit extends AssetTypeInfo { val typeId = 80; def geometryType = "linear"; val label = "LengthLimit"; val layerName = "lengthLimit" }
case object WidthLimit extends AssetTypeInfo { val typeId = 90; def geometryType = "linear"; val label = "WidthLimit"; val layerName = "widthLimit" }
case object LitRoad extends AssetTypeInfo { val typeId = 100; def geometryType = "linear"; val label = "LitRoad"; val layerName = "litRoad" }
case object PavedRoad extends AssetTypeInfo { val typeId = 110; def geometryType = "linear"; val label = "PavedRoad"; val layerName = "pavedRoad" }
case object RoadWidth extends AssetTypeInfo { val typeId = 120; def geometryType = "linear"; val label =  "RoadWidth"; val layerName = "roadWidth"}
case object DamagedByThaw extends AssetTypeInfo { val typeId = 130; def geometryType = "linear"; val label = "DamagedByThaw"; val layerName = "roadDamagedByThaw" }
case object NumberOfLanes extends AssetTypeInfo { val typeId = 140; def geometryType = "linear"; val label = "NumberOfLanes"; val layerName = "numberOfLanes" }
case object MassTransitLane extends AssetTypeInfo { val typeId = 160; def geometryType = "linear"; val label = "MassTransitLane"; val layerName = "massTransitLanes"  }
case object TrafficVolume extends AssetTypeInfo { val typeId = 170; def geometryType = "linear"; val label = "TrafficVolume"; val layerName = "trafficVolume" }
case object WinterSpeedLimit extends AssetTypeInfo { val typeId = 180; def geometryType = "linear"; val label = "WinterSpeedLimit"; val layerName = "winterSpeedLimits"  }
case object Prohibition extends AssetTypeInfo { val typeId = 190; def geometryType = "linear"; val label = ""; val layerName = "prohibition" }
case object PedestrianCrossings extends AssetTypeInfo { val typeId = 200; def geometryType = "point"; val label = "PedestrianCrossings"; val layerName = "pedestrianCrossings" }
case object HazmatTransportProhibition extends AssetTypeInfo { val typeId = 210; def geometryType = "linear"; val label = "HazmatTransportProhibition"; val layerName = "hazardousMaterialTransportProhibition" }
case object Obstacles extends AssetTypeInfo { val typeId = 220; def geometryType = "point"; val label = ""; val layerName = "obstacles" }
case object RailwayCrossings extends AssetTypeInfo { val typeId = 230; def geometryType = "point"; val label = ""; val layerName = "railwayCrossings" }
case object DirectionalTrafficSigns extends AssetTypeInfo { val typeId = 240; def geometryType = "point"; val label = ""; val layerName = "directionalTrafficSigns" }
case object ServicePoints extends AssetTypeInfo { val typeId = 250; def geometryType = "point"; val label = ""; val layerName = "servicePoints" }
case object EuropeanRoads extends AssetTypeInfo { val typeId = 260; def geometryType = "linear"; val label = ""; val layerName = "europeanRoads" }
case object ExitNumbers extends AssetTypeInfo { val typeId = 270; def geometryType = "linear"; val label = ""; val layerName = "exitNumbers" }
case object TrafficLights extends AssetTypeInfo { val typeId = 280; def geometryType = "point"; val label =  ""; val layerName = "trafficLights"}
case object MaintenanceRoadAsset extends AssetTypeInfo { val typeId = 290; def geometryType = "linear"; val label = ""; val layerName = "maintenanceRoad" }
case object TrafficSigns extends AssetTypeInfo { val typeId = 300; def geometryType = "point"; val label = ""; val layerName = "trafficSigns"}
case object StateSpeedLimit extends AssetTypeInfo { val typeId = 310; def geometryType = "linear"; val label = "StateSpeedLimit"; val layerName = "totalWeightLimit" }
case object UnknownAssetTypeId extends  AssetTypeInfo {val typeId = 99; def geometryType = ""; val label = ""; val layerName = ""}
case object TrWidthLimit extends  AssetTypeInfo {val typeId = 370; def geometryType = "point"; val label = "TrWidthLimit"; val layerName = "trWidthLimits"}
case object TrHeightLimit extends  AssetTypeInfo {val typeId = 360; def geometryType = "point"; val label = "TrHeightLimit"; val layerName = "trHeightLimits"}
case object TrTrailerTruckWeightLimit extends  AssetTypeInfo {val typeId = 330; def geometryType = "point"; val label = "TrTrailerTruckWeightLimit"; val layerName = "trWeightLimits"}
case object TrBogieWeightLimit extends  AssetTypeInfo {val typeId = 350; def geometryType = "point"; val label = "TrBogieWeightLimit"; val layerName = "trWeightLimits"}
case object TrAxleWeightLimit extends  AssetTypeInfo {val typeId = 340; def geometryType = "point"; val label = "TrAxleWeightLimit"; val layerName = "trWeightLimits"}
case object TrWeightLimit extends  AssetTypeInfo {val typeId = 320; def geometryType = "point"; val label = "TrWeightLimit"; val layerName = "trWeightLimits"}
case object Manoeuvres extends AssetTypeInfo { val typeId = 380; def geometryType = "linear"; val label = "Manoeuvre"; val layerName = "manoeuvres" }
case object CareClass extends  AssetTypeInfo {val typeId = 390; def geometryType = "linear"; val label = "CareClass"; val layerName = "careClass"}
case object CarryingCapacity extends AssetTypeInfo { val typeId = 400; def geometryType = "linear"; val label = "CarryingCapacity" ; val layerName = "carryingCapacity"}

object AutoGeneratedValues {
  val allAutoGeneratedValues =
    Seq(
      "dr1conversion",
      "dr1_conversion",
      "automatic_correction",
      "excel_data_migration",
      "automatic_generation",
      "vvh_generated",
      "vvh_modified",
      "vvh_mtkclass_default"
    )
}