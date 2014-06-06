package fi.liikennevirasto.digiroad2

import org.scalatest.Tag
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.asset.AssetStatus._
import fi.liikennevirasto.digiroad2.authentication.SessionApi
import fi.liikennevirasto.digiroad2.asset.EnumeratedPropertyValue
import fi.liikennevirasto.digiroad2.asset.AssetWithProperties
import scala.Some
import fi.liikennevirasto.digiroad2.asset.PropertyValue

class Digiroad2ApiSpec extends AuthenticatedApiSpec {
  protected implicit val jsonFormats: Formats = DefaultFormats
  val TestPropertyId = "katos"
  val TestPropertyId2 = "pysakin_tyyppi"
  val CreatedTestAssetId = 300004

  addServlet(classOf[Digiroad2Api], "/*")
  addServlet(classOf[SessionApi], "/auth/*")

  test("require authentication", Tag("db")) {
    get("/assets?assetTypeId=10&bbox=374702,6677462,374870,6677780&municipalityNumber=235") {
      status should equal(401)
    }
    getWithUserAuth("/assets?assetTypeId=10&bbox=374702,6677462,374870,6677780&municipalityNumber=235", username = "nonexistent") {
      status should equal(403)
    }
    getWithUserAuth("/assets?assetTypeId=10&bbox=374702,6677462,374870,6677780&municipalityNumber=235", username = "test") {
      status should equal(200)
    }
  }

  test("provide header to indicate session still active", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=374702,6677462,374870,6677780&validityPeriod=current") {
      status should equal(200)
      response.getHeader(Digiroad2Context.Digiroad2ServerOriginatedResponseHeader) should be ("true")
    }
  }

  test("get assets", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=374650,6677400,374920,6677820&validityPeriod=current") {
      status should equal(200)
      parse(body).extract[List[Asset]].size should be(1)
    }
  }

  test("get assets without bounding box", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=373305,6676648,375755,6678084") {
      status should equal(200)
      parse(body).extract[List[Asset]].filterNot(_.id == 300000).size should be(4)
    }
  }

  test("get assets without bounding box for multiple municipalities", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=373305,6676648,375755,6678084", "test2") {
      status should equal(200)
      parse(body).extract[List[Asset]].filterNot(_.id == 300000).size should be(5)
    }
  }

  test("get asset by id", Tag("db")) {
    getWithUserAuth("/assets/" + CreatedTestAssetId) {
      status should equal(200)
      parse(body).extract[AssetWithProperties].id should be (CreatedTestAssetId)
    }
    getWithUserAuth("/assets/9999999999999999") {
      status should equal(404)
    }
  }

  test("get enumerated property values", Tag("db")) {
    getWithUserAuth("/enumeratedPropertyValues/10") {
      status should equal(200)
      parse(body).extract[List[EnumeratedPropertyValue]].size should be(11)
    }
  }

  test("get map configuration", Tag("db")) {
    getWithUserAuth("/config") {
      status should equal(200)
      val responseJson = parse(body)
      (responseJson \ "mapfull" \ "state" \ "zoom").values should equal(8)
      (responseJson \ "mapfull" \ "state" \ "east").values should equal("373560.0")
      (responseJson \ "mapfull" \ "state" \ "north").values should equal("6677676.0")
    }
  }

  test("get road links", Tag("db")) {
    getWithUserAuth("/roadlinks?bbox=374662,6677430,374890,6677800") {
      status should equal(200)
      val roadLinksJson = parse(body)
      roadLinksJson.children.size should be (21)
      val rl = roadLinksJson.children.head
      (rl \ "type").extract[String] should be ("Street")
    }
  }

  test("update asset property", Tag("db")) {
    val body1 = propertiesToJson(SimpleProperty(TestPropertyId2, Seq(PropertyValue("3"))))
    val body2 = propertiesToJson(SimpleProperty(TestPropertyId2, Seq(PropertyValue("2"))))
    putJsonWithUserAuth("/assets/" + CreatedTestAssetId, body1.getBytes) {
      status should equal(200)
      getWithUserAuth("/assets/" + CreatedTestAssetId) {
        val asset = parse(body).extract[AssetWithProperties]
        val prop = asset.propertyData.find(_.publicId == TestPropertyId2).get
        prop.values.size should be (1)
        prop.values.head.propertyValue should be ("3")
        putJsonWithUserAuth("/assets/" + CreatedTestAssetId, body2.getBytes) {
          status should equal(200)
          getWithUserAuth("/assets/" + CreatedTestAssetId) {
            parse(body).extract[AssetWithProperties].propertyData.find(_.publicId == TestPropertyId2).get.values.head.propertyValue should be ("2")
          }
        }
      }
    }
  }

  test("validate asset when creating", Tag("db")) {
    val requestPayload = """{"assetTypeId": 10, "lon": 0, "lat": 0, "roadLinkId": 5990, "bearing": 0}"""
    postJsonWithUserAuth("/assets", requestPayload.getBytes) {
      status should equal(500)
    }
  }

  test("get past assets", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=374702,6677462,374870,6677780&validityPeriod=past") {
      status should equal(200)
      parse(body).extract[List[Asset]].size should be(1)
    }
  }

  test("assets cannot be retrieved without bounding box", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10") {
      status should equal(500)
    }
  }

  test("assets cannot be retrieved with massive bounding box", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=324702,6677462,374870,6697780") {
      status should equal(500)
    }
  }

  test("return failure if bounding box missing", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=374702,6677462,374870,6677780&validityPeriod=past") {
      status should equal(200)
      parse(body).extract[List[Asset]].size should be(1)
    }
  }

  test("get future assets", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=374702,6677462,374870,6677780&validityPeriod=future") {
      status should equal(200)
      parse(body).extract[List[Asset]].size should be(1)
    }
  }

  test("mark asset on expired link as floating", Tag("db")) {
    getWithUserAuth("/assets?assetTypeId=10&bbox=373305,6676648,375755,6678084&validityDate=2014-06-01&validityPeriod=current", "test49") {
      status should equal(200)
      val assets = parse(body).extract[List[Asset]]
      assets should have length 1
      assets.head.status should be(Some(Floating))
     }
  }

  test("load image by id", Tag("db")) {
    getWithUserAuth("/images/2") {
      status should equal(200)
      body.length should(be > 0)
    }
  }

  test("load image by id and timestamp", Tag("db")) {
    getWithUserAuth("/images/1_123456789") {
      status should equal(200)
      body.length should(be > 0)
    }
  }

  test("write requests pass only if user is not in viewer role", Tag("db")) {
    postJsonWithUserAuth("/assets/", Array(), username = "testviewer") {
      status should equal(401)
    }
    postJsonWithUserAuth("/assets/", Array()) {
      // no asset id given, returning 404 is legal
      status should equal(404)
    }
  }

  test("get available properties for asset type", Tag("db")) {
    getWithUserAuth("/assetTypeProperties/10") {
      status should equal(200)
      val ps = parse(body).extract[List[Property]]
      ps.size should equal(31)
      val p1 = ps.find(_.publicId == TestPropertyId).get
      p1.publicId should be ("katos")
      p1.propertyType should be ("single_choice")
      p1.required should be (true)
      ps.find(_.publicId == "vaikutussuunta") should be ('defined)
    }
  }

  test("get localized property names for language") {
    // propertyID -> value
    getWithUserAuth("/assetPropertyNames/fi") {
      status should equal(200)
      val propertyNames = parse(body).extract[Map[String, String]]
      propertyNames("ensimmainen_voimassaolopaiva") should be("Ensimmäinen voimassaolopäivä")
      propertyNames("matkustajatunnus") should be("Matkustajatunnus")
    }
  }

  test("get national bus stop id", Tag("db")) {
    getWithUserAuth("/assets/300008") {
      val assetWithProperties: AssetWithProperties = parse(body).extract[AssetWithProperties]
      assetWithProperties.externalId should be (85755)
    }
  }

  test("asset properties are in same order when creating new or editing existing", Tag("db")) {
    val propIds = getWithUserAuth("/assetTypeProperties/10") {
      parse(body).extract[List[Property]].map(p => p.publicId)
    }
    val assetPropNames = getWithUserAuth("/assets/" + CreatedTestAssetId) {
      parse(body).extract[AssetWithProperties].propertyData.map(p => p.publicId)
    }
    propIds should be(assetPropNames)
  }

  private[this] def propertiesToJson(prop: SimpleProperty): String = {
    val json = write(Seq(prop))
    s"""{"properties":$json}"""
  }
}
