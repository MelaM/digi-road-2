package fi.liikennevirasto.digiroad2.dao.pointasset

import fi.liikennevirasto.digiroad2.dao.Queries._
import fi.liikennevirasto.digiroad2.{GeometryUtils, PersistedPoint, PersistedPointAsset, Point}
import org.joda.time.DateTime
import slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import fi.liikennevirasto.digiroad2.asset.{BoundingRectangle, LinkGeomSource}
import fi.liikennevirasto.digiroad2.dao.{Queries, Sequences}
import fi.liikennevirasto.digiroad2.service.pointasset.IncomingObstacle
import slick.jdbc.StaticQuery.interpolation
import slick.jdbc.{GetResult, PositionedResult, StaticQuery}
import com.github.tototoshi.slick.MySQLJodaSupport._
import fi.liikennevirasto.digiroad2.dao.pointasset.OracleDirectionalTrafficSignDao.fetchByFilter
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase

case class Obstacle(id: Long, linkId: Long,
                    lon: Double, lat: Double,
                    mValue: Double, floating: Boolean,
                    vvhTimeStamp: Long,
                    municipalityCode: Int,
                    obstacleType: Int,
                    createdBy: Option[String] = None,
                    createdAt: Option[DateTime] = None,
                    modifiedBy: Option[String] = None,
                    modifiedAt: Option[DateTime] = None,
                    expired: Boolean = false,
                    linkSource: LinkGeomSource) extends PersistedPoint

case class ObstacleShapefile(lon: Double, lat: Double, obstacleType: Int = 1)

object OracleObstacleDao {

  private def query() = {
    """
      select a.id, pos.link_id, a.geometry, pos.start_measure, a.floating, pos.adjusted_timestamp, a.municipality_code, ev.value, a.created_by, a.created_date, a.modified_by,
      a.modified_date, case when a.valid_to <= sysdate then 1 else 0 end as expired, pos.link_source
      from asset a
      join asset_link al on a.id = al.asset_id
      join lrm_position pos on al.position_id = pos.id
      join property p on p.asset_type_id = a.asset_type_id
      left join single_choice_value scv on scv.asset_id = a.id
      left join enumerated_value ev on (ev.property_id = p.id AND scv.enumerated_value_id = ev.id)
    """
  }

  def fetchByFilterWithExpired(queryFilter: String => String): Seq[Obstacle] = {
    val queryWithFilter = queryFilter(query())
    StaticQuery.queryNA[Obstacle](queryWithFilter).iterator.toSeq
  }

  // This works as long as there is only one (and exactly one) property (currently type) for obstacles and up to one value
  def fetchByFilter(queryFilter: String => String, withDynSession: Boolean = false): Seq[Obstacle] = {
    val queryWithFilter = queryFilter(query()) + " and (a.valid_to > sysdate or a.valid_to is null)"
    if(withDynSession){
      OracleDatabase.withDynSession {
        StaticQuery.queryNA[Obstacle](queryWithFilter).iterator.toSeq
      }
    } else {
      StaticQuery.queryNA[Obstacle](queryWithFilter).iterator.toSeq
    }
  }

  implicit val getPointAsset = new GetResult[Obstacle] {
    def apply(r: PositionedResult) = {
      val id = r.nextLong()
      val linkId = r.nextLong()
      val point = r.nextBytesOption().map(bytesToPoint).get
      val mValue = r.nextDouble()
      val floating = r.nextBoolean()
      val vvhTimeStamp = r.nextLong()
      val municipalityCode = r.nextInt()
      val obstacleType = r.nextInt()
      val createdBy = r.nextStringOption()
      val createdDateTime = r.nextTimestampOption().map(timestamp => new DateTime(timestamp))
      val modifiedBy = r.nextStringOption()
      val modifiedDateTime = r.nextTimestampOption().map(timestamp => new DateTime(timestamp))
      val expired = r.nextBoolean()
      val linkSource = r.nextInt()

      Obstacle(id, linkId, point.x, point.y, mValue, floating, vvhTimeStamp, municipalityCode, obstacleType, createdBy, createdDateTime, modifiedBy, modifiedDateTime, expired, LinkGeomSource(linkSource))
    }
  }

  def create(obstacle: IncomingObstacle, mValue: Double, username: String, municipality: Int, adjustmentTimestamp: Long, linkSource: LinkGeomSource): Long = {
    val id = Sequences.nextPrimaryKeySeqValue
    val lrmPositionId = Sequences.nextLrmPositionPrimaryKeySeqValue
    sqlu"""
      insert all
        into asset(id, asset_type_id, created_by, created_date, municipality_code)
        values ($id, 220, $username, sysdate, $municipality)

        into lrm_position(id, start_measure, link_id, adjusted_timestamp, link_source)
        values ($lrmPositionId, $mValue, ${obstacle.linkId}, $adjustmentTimestamp, ${linkSource.value})

        into asset_link(asset_id, position_id)
        values ($id, $lrmPositionId)

      select * from dual
    """.execute
    updateAssetGeometry(id, Point(obstacle.lon, obstacle.lat))
    insertSingleChoiceProperty(id, getPropertyId, obstacle.obstacleType).execute
    id
  }

  def create(obstacle: IncomingObstacle, mValue: Double, username: String, municipality: Int, adjustmentTimestamp: Long, linkSource: LinkGeomSource, createdByFromUpdate: Option[String] = Some(""), createdDateTimeFromUpdate: Option[DateTime]): Long = {
    val id = Sequences.nextPrimaryKeySeqValue
    val lrmPositionId = Sequences.nextLrmPositionPrimaryKeySeqValue
    sqlu"""
      insert all
        into asset(id, asset_type_id, created_by, created_date, municipality_code, modified_by, modified_date)
        values ($id, 220, $createdByFromUpdate, $createdDateTimeFromUpdate, $municipality, $username, sysdate)

        into lrm_position(id, start_measure, link_id, adjusted_timestamp, link_source, modified_date)
        values ($lrmPositionId, $mValue, ${obstacle.linkId}, $adjustmentTimestamp, ${linkSource.value}, sysdate)

        into asset_link(asset_id, position_id)
        values ($id, $lrmPositionId)

      select * from dual
    """.execute
    updateAssetGeometry(id, Point(obstacle.lon, obstacle.lat))
    insertSingleChoiceProperty(id, getPropertyId, obstacle.obstacleType).execute
    id
  }

  def update(id: Long, obstacle: IncomingObstacle, mValue: Double, username: String, municipality: Int, adjustedTimeStampOption: Option[Long] = None, linkSource: LinkGeomSource) = {
    sqlu""" update asset set municipality_code = $municipality where id = $id """.execute
    updateAssetModified(id, username).execute
    updateAssetGeometry(id, Point(obstacle.lon, obstacle.lat))
    updateSingleChoiceProperty(id, getPropertyId, obstacle.obstacleType).execute

    adjustedTimeStampOption match {
      case Some(adjustedTimeStamp) =>
        sqlu"""
          update lrm_position
           set
           start_measure = $mValue,
           link_id = ${obstacle.linkId},
           adjusted_timestamp = ${adjustedTimeStamp},
           link_source = ${linkSource.value}
          where id = (select position_id from asset_link where asset_id = $id)
        """.execute
      case _ =>
        sqlu"""
          update lrm_position
           set
           start_measure = $mValue,
           link_id = ${obstacle.linkId},
           link_source = ${linkSource.value}
          where id = (select position_id from asset_link where asset_id = $id)
        """.execute
    }

    id
  }

  def selectFloatings(floating: Int, lastIdUpdate: Long, batchSize: Int) : Seq[Obstacle] ={
    val query =
      """
        select * from (
          select a.id, pos.link_id, a.geometry, pos.start_measure, a.floating, pos.adjusted_timestamp, a.municipality_code, ev.value, a.created_by, a.created_date, a.modified_by,
          a.modified_date, case when a.valid_to <= sysdate then 1 else 0 end as expired, pos.link_source
            from asset a
            join asset_link al on a.id = al.asset_id
            join lrm_position pos on al.position_id = pos.id
            join property p on p.asset_type_id = a.asset_type_id
            left join single_choice_value scv on scv.asset_id = a.id
            left join enumerated_value ev on (ev.property_id = p.id AND scv.enumerated_value_id = ev.id)
    """

    val queryWithFilter = query + s"where a.asset_type_id = 220 and a.floating = $floating and " +
      s"(a.valid_to > sysdate or a.valid_to is null) and a.id > $lastIdUpdate order by a.id asc) where ROWNUM <= $batchSize"
    StaticQuery.queryNA[Obstacle](queryWithFilter).iterator.toSeq
  }

  def updateFloatingAsset(obstacleUpdated: Obstacle) = {
    val id = obstacleUpdated.id

    sqlu"""update asset set municipality_code = ${obstacleUpdated.municipalityCode}, floating =  ${obstacleUpdated.floating} where id = $id""".execute

    updateAssetModified(id, obstacleUpdated.modifiedBy.get).execute
    updateAssetGeometry(id, Point(obstacleUpdated.lon, obstacleUpdated.lat))
    updateSingleChoiceProperty(id, getPropertyId, obstacleUpdated.obstacleType).execute

    sqlu"""
      update lrm_position
       set
       start_measure = ${obstacleUpdated.mValue},
       link_id = ${obstacleUpdated.linkId}
       where id = (select position_id from asset_link where asset_id = $id)
    """.execute
  }

  private def getPropertyId: Long = {
    StaticQuery.query[String, Long](Queries.propertyIdByPublicId).apply("esterakennelma").first
  }

  implicit val getObstacleShapefile = new GetResult[ObstacleShapefile] {
    def apply(r: PositionedResult) = {
      val lon = r.nextDouble()
      val lat = r.nextDouble()

      ObstacleShapefile(lon, lat)
    }
  }

  def getObstaclesFromShapefileTable(): Seq[ObstacleShapefile] = {
    val query = """SELECT X_KOORDI, Y_KOORDI FROM DRSTA_PUUTTUVAT_VVH_ESTEET"""
    StaticQuery.queryNA[ObstacleShapefile](query).iterator.toSeq
  }
}



