package fi.liikennevirasto.digiroad2

import org.scalatra.test.scalatest._
import org.scalatest.{FunSuite, Tag}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write
import fi.liikennevirasto.digiroad2.feature.{PropertyValue, AssetType, Asset, BusStop}
import fi.liikennevirasto.digiroad2.feature.{EnumeratedPropertyValue, AssetType, Asset, BusStop}
import org.json4s.JsonDSL._

class Digiroad2ApiSpec extends FunSuite with ScalatraSuite  {
  protected implicit val jsonFormats: Formats = DefaultFormats
  val TestAssetId = 809
  val TestPropertyId = "764"

  addServlet(classOf[Digiroad2Api], "/*")

  test("get bus stops", Tag("db")) {
    get("/busstops?municipalityNumber=235") {
      status should equal (200)
      val busStops = parse(body).extract[List[BusStop]]
      busStops.size should be (41)
    }
  }

  test("get assets", Tag("db")) {
    get("/assets?assetTypeId=10&municipalityNumber=235") {
      status should equal(200)
      parse(body).extract[List[Asset]].size should be(41)
    }
  }

  test("get asset by id", Tag("db")) {
    get("/assets/809") {
      status should equal(200)
      parse(body).extract[Asset].id should be (809)
    }
    get("/assets/9999999999999999") {
      status should equal(404)
    }
  }

  test("get asset types", Tag("db")) {
    get("/assetTypes") {
      status should equal(200)
      parse(body).extract[List[AssetType]].size should be(1)
    }
  }

  test("get enumerated property values", Tag("db")) {
    get("/enumeratedPropertyValues/10") {
      status should equal(200)
      parse(body).extract[List[EnumeratedPropertyValue]].size should be(4)
    }
  }

  test("missing bus stops", Tag("db")) {
    get("/busstops?municipalityNumber=234") {
      status should equal (200)
      parse(body).extract[List[BusStop]].size should be (0)
    }
  }

  test("ping") {
    get("/ping") {
      status should equal (200)
      body should equal ("pong")
    }
  }

  test("get map configuration") {
    get("/config") {
      status should equal(200)
      val responseJson = parse(body)
      (responseJson \ "mapfull" \ "state" \ "zoom").values should equal(8)
      (responseJson \ "mapfull" \ "state" \ "east").values should equal("373560")
      (responseJson \ "mapfull" \ "state" \ "north").values should equal("6677676")
    }
  }

  test("get road links", Tag("db")) {
    get("/roadlinks?municipalityNumber=235") {
      status should equal(200)
      val roadLinksJson = parse(body)
      (roadLinksJson \ "features" \ "geometry").children.size should (be > 500)
      val cs = (roadLinksJson \ "features" \ "geometry" \ "coordinates" \\ classOf[JDouble])
      cs.take(2) should equal (List(373426.924, 6677127.533))
    }
  }

  test("update asset property", Tag("db")) {
    val body1 = write(List(PropertyValue(3, "Linja-autojen kaukoliikenne")))
    val body2 = write(List(PropertyValue(2, "Linja-autojen paikallisliikenne")))
    put("/assets/" + TestAssetId + "/properties/764/values", body1.getBytes, Map("Content-type" -> "application/json")) {
      status should equal(200)
      get("/assets/" + TestAssetId) {
        val asset = parse(body).extract[Asset]
        val prop = asset.propertyData.find(_.propertyId == TestPropertyId).get
        prop.values.size should be (1)
        prop.values.head.propertyValue should be (3)
        put("/assets/" + TestAssetId + "/properties/764/values", body2.getBytes, Map("Content-type" -> "application/json")) {
          status should equal(200)
          get("/assets/" + TestAssetId) {
            parse(body).extract[Asset].propertyData.find(_.propertyId == TestPropertyId).get.values.head.propertyValue should be (2)
          }
        }
      }
    }
  }

  test("delete and create asset property", Tag("db")) {
    val propBody = write(List(PropertyValue(2, "Linja-autojen paikallisliikenne")))
    delete("/assets/" + TestAssetId + "/properties/760/values") {
      status should equal(200)
      get("/assets/" + TestAssetId) {
        val asset = parse(body).extract[Asset]
        asset.propertyData.find(_.propertyId == "760").get.values.size should be (0)
        put("/assets/" + TestAssetId + "/properties/760/values", propBody.getBytes, Map("Content-type" -> "application/json")) {
          status should equal(200)
          get("/assets/" + TestAssetId) {
            parse(body).extract[Asset].propertyData.find(_.propertyId == TestPropertyId).get.values.head.propertyValue should be (2)
          }
        }
      }
    }
  }
}
