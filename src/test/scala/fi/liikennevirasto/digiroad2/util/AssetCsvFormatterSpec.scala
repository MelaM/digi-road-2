package fi.liikennevirasto.digiroad2.util

import org.scalatest._
import fi.liikennevirasto.digiroad2.asset.AssetWithProperties
import fi.liikennevirasto.digiroad2.asset.oracle.OracleSpatialAssetProvider
import fi.liikennevirasto.digiroad2.user.oracle.OracleUserProvider

class AssetCsvFormatterSpec extends FlatSpec with MustMatchers with BeforeAndAfter with BeforeAndAfterAll {
  val userProvider = new OracleUserProvider
  val provider = new OracleSpatialAssetProvider(userProvider)
  var assetsByMunicipality: Iterable[AssetWithProperties] = null
  before {
    assetsByMunicipality = provider.getAssetsByMunicipality(235)
  }

  it must "return correct csv entries from test data" in {
    val csvAll = AssetCsvFormatter.formatAssetsWithProperties(assetsByMunicipality)
    csvAll.size must be > 3
    val csv = csvAll.find(_.startsWith("5")).get

    val propertyValue = extractPropertyValue(assetsByMunicipality.find(_.externalId.get == 5).get, _: String)
    val created = parseCreated(propertyValue("lisatty_jarjestelmaan"))

    val validFrom = propertyValue("ensimmainen_voimassaolopaiva")
    val validTo = propertyValue("viimeinen_voimassaolopaiva")

    csv must equal("5;;;;;374792.096855508;6677566.77442972;;;210;2;;1;1;1;0;;;;" + created + ";dr1conversion;" + validFrom + ";" + validTo + ";Liikennevirasto;235;Kauniainen;;")
  }

  private def extractPropertyValue(asset: AssetWithProperties, propertyPublicId: String): String = {
    asset.propertyData.find(_.publicId == propertyPublicId).get.values.head.propertyDisplayValue.getOrElse("")
  }

  private def parseCreated(s: String): String = {
    s.split(" ").drop(1).mkString(" ")
  }
}
