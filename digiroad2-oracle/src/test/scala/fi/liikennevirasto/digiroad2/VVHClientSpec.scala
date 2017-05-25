package fi.liikennevirasto.digiroad2

import java.util.Properties
import com.vividsolutions.jts.geom.{GeometryFactory}
import fi.liikennevirasto.digiroad2.asset.BoundingRectangle
import org.geotools.geometry.jts.GeometryBuilder
import org.scalatest.{FunSuite, Matchers}

class VVHClientSpec extends FunSuite with Matchers{
  lazy val properties: Properties = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("/digiroad2.properties"))
    props
  }

  val geomFact= new GeometryFactory()
  val geomBuilder = new GeometryBuilder(geomFact)

  /**
    * Checks that VVH history bounding box search works uses API example bounding box so it should receive results
    */
  test("Tries to connect VVH history API and retrive result") {
    val vvhClient= new VVHClient(properties.getProperty("digiroad2.VVHRestApiEndPoint"))
    val result= vvhClient.historyData.fetchVVHRoadlinkHistoryByBoundsAndMunicipalities(BoundingRectangle(Point(564000, 6930000),Point(566000, 6931000)), Set(420))
    result.size should be >1
  }

  test("Fetch roadlinks with polygon string ") {
    val vvhClient= new VVHClient(properties.getProperty("digiroad2.VVHRestApiEndPoint"))
    val result= vvhClient.queryRoadLinksByPolygons(geomBuilder.polygon(564000,6930000,566000,6931000,567000,6933000))
    result.size should be >1
  }

  test("Fetch roadlinks with empty polygon string") {
    val vvhClient= new VVHClient(properties.getProperty("digiroad2.VVHRestApiEndPoint"))
    val result= vvhClient.queryRoadLinksByPolygons(geomBuilder.polygon())
    result.size should be (0)
  }
  /**
    * Checks that VVH history link id search works and returns something
    */
  test("Test VVH History LinkId API") {
    val vvhClient= new VVHClient(properties.getProperty("digiroad2.VVHRestApiEndPoint"))
    val result = vvhClient.historyData.fetchVVHRoadlinkHistory(Set(440484,440606,440405,440489))
    result.nonEmpty should be (true)
  }
  test("Fetch changes with polygon string ") {
    val vvhClient= new VVHClient(properties.getProperty("digiroad2.VVHRestApiEndPoint"))
    val result= vvhClient.queryChangesByPolygon(geomBuilder.polygon(528428,6977212,543648,6977212,543648,7002668,528428,7002668))
    result.size should be >1
  }
  test("Fetch changes with empty polygon string") {
    val vvhClient= new VVHClient(properties.getProperty("digiroad2.VVHRestApiEndPoint"))
    val result= vvhClient.queryChangesByPolygon(geomBuilder.polygon())
    result.size should be (0)
  }
}
