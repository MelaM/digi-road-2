package fi.liikennevirasto.digiroad2.util

import java.util.Properties

import com.vividsolutions.jts.geom._
import fi.liikennevirasto.digiroad2.asset.BoundingRectangle
import fi.liikennevirasto.digiroad2.user.UserProvider
import org.geotools.geometry.jts.GeometryBuilder
import fi.liikennevirasto.digiroad2.Point
import com.vividsolutions.jts.io.WKTReader
import scala.collection.mutable.ListBuffer

/**
  * Tools related to polygons
  */
class PolygonTools {
  val geomFact = new GeometryFactory()
  val geomBuilder = new GeometryBuilder(geomFact)
  lazy val properties: Properties = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("/digiroad2.properties"))
    props
  }
  lazy val userProvider: UserProvider = {
    Class.forName(properties.getProperty("digiroad2.userProvider")).newInstance().asInstanceOf[UserProvider]
  }

  /**
    *
    * @param geometry jts Geometry
    * @param boundingBox  BoundingRectangle
    * @return returns Sequence of JTS Polygons that are with in bounding box
    */
  def geometryInterceptorToBoundingBox(geometry: Geometry, boundingBox: BoundingRectangle): Seq[Polygon] = {
    val leftBottomP = boundingBox.leftBottom
    val rightTopP = boundingBox.rightTop
    val leftTopP = Point(leftBottomP.x, rightTopP.y)
    val rightBottom = Point(rightTopP.x, leftBottomP.y)
    val BoundingBoxAsPoly = geomBuilder.polygon(leftTopP.x, leftTopP.y, rightTopP.x, rightTopP.y, rightBottom.x, rightBottom.y, leftBottomP.x, leftBottomP.y)
    val intersectionGeometry = geometry.intersection(BoundingBoxAsPoly)
    if (intersectionGeometry.getGeometryType.toLowerCase.startsWith("polygon")) {
      Seq(intersectionGeometry.asInstanceOf[Polygon])
    } else if (intersectionGeometry.isEmpty) {
      Seq.empty[Polygon]
    } else if (intersectionGeometry.getGeometryType.toLowerCase.contains("multipolygon")) {
      multiPolygonToPolygonSeq(intersectionGeometry.asInstanceOf[MultiPolygon])
    } else
      Seq.empty[Polygon]
  }

  def getPolygonByArea(areaId: Int): Seq[Polygon] = {
    val geometry = getAreaGeometry(areaId)

    val polygon = geometry match {
      case _ if geometry.getGeometryType.toLowerCase.startsWith("polygon") =>
        Seq(geometry.asInstanceOf[Polygon])
      case _ if geometry.getGeometryType.toLowerCase.startsWith("multipolygon") =>
        multiPolygonToPolygonSeq(geometry.asInstanceOf[MultiPolygon])
      case _ => Seq.empty[Polygon]
    }
    polygon
  }

  def getAreaGeometry(areaId: Int): Geometry = {
    val wKTParser = new WKTReader()
    val areaChoose= new getServiceArea()
    wKTParser.read(areaChoose.getArea(areaId))
  }

  def multiPolygonToPolygonSeq (multiPoly: MultiPolygon): Seq[Polygon] ={
    var geomCounter=multiPoly.getNumGeometries
    var  listPolygons= ListBuffer.empty[Polygon]
    while (geomCounter>0)
    {
      val poly=multiPoly.getGeometryN(geomCounter-1)
      if (poly.getGeometryType=="Polygon") {
        listPolygons += poly.asInstanceOf[Polygon]
      }
      geomCounter-=1
    }
    listPolygons
  }
}