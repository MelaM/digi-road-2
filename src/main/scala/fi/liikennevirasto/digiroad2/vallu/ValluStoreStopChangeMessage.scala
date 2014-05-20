package fi.liikennevirasto.digiroad2.vallu

import fi.liikennevirasto.digiroad2.asset.{PropertyTypes, PropertyValue, Property, AssetWithProperties}
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}

object ValluStoreStopChangeMessage {

  def create(municipalityName: String, asset: AssetWithProperties): String = {
    """<?xml version="1.0" encoding="UTF-8"?>""" +
    (<Stops xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <Stop>
        <StopId>{asset.externalId.get}</StopId>
        <AdminStopId>{if (propertyIsDefined(asset, "yllapitajan_tunnus")) extractPropertyValue(asset, "yllapitajan_tunnus") }</AdminStopId>
        <StopCode>{if (propertyIsDefined(asset, "matkustajatunnus")) extractPropertyValue(asset, "matkustajatunnus") }</StopCode>
        { if (localizedNameIsDefined(asset))
            <Names>
            { if (propertyIsDefined(asset, "nimi_suomeksi")) <Name lang="fi">{extractPropertyValue(asset, "nimi_suomeksi") }</Name> }
            { if (propertyIsDefined(asset, "nimi_ruotsiksi")) <Name lang="sv">{extractPropertyValue(asset, "nimi_ruotsiksi") }</Name> }
            </Names>
        }
        <Coordinate>
          <xCoordinate>{asset.wgslon.toInt}</xCoordinate>
          <yCoordinate>{asset.wgslat.toInt}</yCoordinate>
        </Coordinate>
        <Bearing>{asset.bearing.flatMap { bearing =>
          asset.validityDirection.map { validityDirection =>
            ValluTransformer.calculateActualBearing(validityDirection, bearing)
          }
        }.getOrElse("")}</Bearing>
        { if (propertyIsDefined(asset, "liikennointisuuntima")) <BearingDescription>{ extractPropertyDisplayValue(asset, "liikennointisuuntima") }</BearingDescription>}
        { if (propertyIsDefined(asset, "liikennointisuunta")) <Direction>{extractPropertyValue(asset, "liikennointisuunta") }</Direction> }
        <StopAttribute>{getBusstopBlock(asset)}</StopAttribute>
        <Equipment>{ValluTransformer.getEquipment(asset)}</Equipment>
        <Reachability>{ValluTransformer.getReachability(asset)}</Reachability>
        <SpecialNeeds>{if (propertyIsDefined(asset, "esteettomyys_liikuntarajoitteiselle")) extractPropertyValue(asset, "esteettomyys_liikuntarajoitteiselle") }</SpecialNeeds>
        { val modification = asset.modified.modificationTime match {
            case Some(_) => asset.modified
            case _ => asset.created
          }
          <ModifiedTimestamp>{ISODateTimeFormat.dateHourMinuteSecond.print(modification.modificationTime.get)}</ModifiedTimestamp>
          <ModifiedBy>{modification.modifier.get}</ModifiedBy>
        }
        {if (! propertyIsDefined(asset, "ensimmainen_voimassaolopaiva") || propertyIsEmpty(asset, "ensimmainen_voimassaolopaiva"))
            <ValidFrom xsi:nil="true" />
         else
            <ValidFrom>{ValluTransformer.transformToISODate(ValluTransformer.extractPropertyValueOption(asset, "ensimmainen_voimassaolopaiva"))}</ValidFrom>}
        {if (! propertyIsDefined(asset, "viimeinen_voimassaolopaiva") || propertyIsEmpty(asset, "viimeinen_voimassaolopaiva"))
          <ValidTo xsi:nil="true" />
         else
          <ValidTo>{ValluTransformer.transformToISODate(ValluTransformer.extractPropertyValueOption(asset, "viimeinen_voimassaolopaiva"))}</ValidTo>}
        <AdministratorCode>{extractPropertyDisplayValue(asset, "tietojen_yllapitaja")}</AdministratorCode>
        <MunicipalityCode>{asset.municipalityNumber.get}</MunicipalityCode>
        <MunicipalityName>{municipalityName}</MunicipalityName>
        <Comments>{if (propertyIsDefined(asset, "lisatiedot")) extractPropertyValue(asset, "lisatiedot") }</Comments>
        <ContactEmails>
          <Contact>pysakit@liikennevirasto.fi</Contact>
        </ContactEmails>
      </Stop>
    </Stops>).toString()
  }

  def getBusstopBlock(asset: AssetWithProperties) = {
    val busStopTypes = ValluTransformer.getBusStopTypes(asset)
    <StopType name="LOCAL_BUS">{busStopTypes._1}</StopType>
    <StopType name="EXPRESS_BUS">{busStopTypes._2}</StopType>
    <StopType name="NON_STOP_EXPRESS_BUS">{busStopTypes._3}</StopType>
    <StopType name="VIRTUAL_STOP">{busStopTypes._4}</StopType>
  }

  private def localizedNameIsDefined(asset: AssetWithProperties): Boolean = {
    propertyIsDefined(asset, "nimi_suomeksi") || propertyIsDefined(asset, "nimi_ruotsiksi")
  }

  private def propertyIsDefined(asset: AssetWithProperties, propertyPublicId: String): Boolean = {
    ValluTransformer.extractPropertyValueOption(asset, propertyPublicId).isDefined
  }

  private def propertyIsEmpty(asset: AssetWithProperties, propertyPublicId: String): Boolean = {
    propertyIsDefined(asset, propertyPublicId) && extractPropertyValue(asset, propertyPublicId).isEmpty
  }

  private def extractPropertyValue(asset: AssetWithProperties, propertyPublicId: String): String = {
    ValluTransformer.extractPropertyValueOption(asset, propertyPublicId).get
  }

  private def extractPropertyDisplayValue(asset: AssetWithProperties, propertyPublicId: String): String = {
    asset.propertyData.find(property => property.publicId == propertyPublicId)
      .head.values.head.propertyDisplayValue.get
  }
}
