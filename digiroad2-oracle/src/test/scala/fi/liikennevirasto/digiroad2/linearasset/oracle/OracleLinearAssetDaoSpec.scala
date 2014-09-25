package fi.liikennevirasto.digiroad2.linearasset.oracle

import fi.liikennevirasto.digiroad2.Point
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase._
import fi.liikennevirasto.digiroad2.asset.oracle.Queries._

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.Tag

import oracle.spatial.geometry.JGeometry
import scala.slick.jdbc.{StaticQuery => Q}
import Q.interpolation
import scala.slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession

class OracleLinearAssetDaoSpec extends FunSuite with Matchers {

  private def truncateLinkGeometry(linkId: Long, startMeasure: Double, endMeasure: Double): Seq[(Double, Double)] = {
    val truncatedGeometry: Array[Byte] = sql"""
      select to_2d(sdo_lrs.dynamic_segment(rl.geom, $startMeasure, $endMeasure))
        from ROAD_LINK rl
        where id = $linkId
        """.as[Array[Byte]].list.head
    val points = JGeometry.load(truncatedGeometry).getOrdinatesArray.grouped(2)
    points.map { pointArray =>
      (pointArray(0), pointArray(1))
    }.toSeq
  }

  def endPoints(geometry: Seq[(Double, Double)]): (Point, Point) = {
    val (firstPointX, firstPointY) = geometry.head
    val (lastPointX, lastPointY) = geometry.last
    (Point(firstPointX, firstPointY), Point(lastPointX, lastPointY))
  }

  def assertSpeedLimitEndPointsOnLink(speedLimitId: Long, roadLinkId: Long, startMeasure: Double, endMeasure: Double) = {
    val expectedEndPoints = endPoints(truncateLinkGeometry(roadLinkId, startMeasure, endMeasure).toList)
    val limitEndPoints = endPoints(OracleLinearAssetDao.getSpeedLimits(speedLimitId).head._2.toList)
    expectedEndPoints._1.distanceTo(limitEndPoints._1) should be(0.0 +- 0.01)
    expectedEndPoints._2.distanceTo(limitEndPoints._2) should be(0.0 +- 0.01)
  }

  test("splitting one link speed limit " +
    "where split measure is after link middle point " +
    "modifies end measure of existing speed limit " +
    "and creates new speed limit for second split", Tag("db")) {
    Database.forDataSource(ds).withDynTransaction {
      val createdId = OracleLinearAssetDao.splitSpeedLimit(700114, 5537, 100, "test")
      val (existingModifiedBy, _, _, _, _) = OracleLinearAssetDao.getSpeedLimitDetails(700114)
      val (_, _, newCreatedBy, _, _) = OracleLinearAssetDao.getSpeedLimitDetails(createdId)

      assertSpeedLimitEndPointsOnLink(700114, 5537, 0, 100)
      assertSpeedLimitEndPointsOnLink(createdId, 5537, 100, 136.788)

      existingModifiedBy shouldBe Some("test")
      newCreatedBy shouldBe Some("test")
      dynamicSession.rollback()
    }
  }

  test("splitting one link speed limit " +
    "where split measure is before link middle point " +
    "modifies start measure of existing speed limit " +
    "and creates new speed limit for first split", Tag("db")) {
    Database.forDataSource(ds).withDynTransaction {
      val createdId = OracleLinearAssetDao.splitSpeedLimit(700114, 5537, 50, "test")
      val (modifiedBy, _, _, _, _) = OracleLinearAssetDao.getSpeedLimitDetails(700114)
      val (_, _, newCreatedBy, _, _) = OracleLinearAssetDao.getSpeedLimitDetails(createdId)

      assertSpeedLimitEndPointsOnLink(700114, 5537, 50, 136.788)
      assertSpeedLimitEndPointsOnLink(createdId, 5537, 0, 50)

      modifiedBy shouldBe Some("test")
      newCreatedBy shouldBe Some("test")
      dynamicSession.rollback()
    }
  }
}
