package fi.liikennevirasto.digiroad2.dao.pointasset

import fi.liikennevirasto.digiroad2.dao.Queries._
import fi.liikennevirasto.digiroad2.{GeometryUtils, PersistedPoint, PersistedPointAsset, Point}
import org.joda.time.DateTime
import slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import fi.liikennevirasto.digiroad2.asset.{BoundingRectangle, LinkGeomSource, PedestrianCrossings}
import fi.liikennevirasto.digiroad2.dao.Sequences
import fi.liikennevirasto.digiroad2.service.pointasset.IncomingPedestrianCrossing

import scala.language.reflectiveCalls
import slick.jdbc.StaticQuery.interpolation
import slick.jdbc.{GetResult, _}
import com.github.tototoshi.slick.MySQLJodaSupport._
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import slick.jdbc

case class PedestrianCrossing(id: Long, linkId: Long,
                              lon: Double, lat: Double,
                              mValue: Double, floating: Boolean,
                              vvhTimeStamp: Long,
                              municipalityCode: Int,
                              createdBy: Option[String] = None,
                              createdAt: Option[DateTime] = None,
                              modifiedBy: Option[String] = None,
                              modifiedAt: Option[DateTime] = None,
                              expired: Boolean = false,
                              linkSource: LinkGeomSource) extends PersistedPoint


class OraclePedestrianCrossingDao() {
  def update(id: Long, persisted: IncomingPedestrianCrossing, mValue: Double, username: String, municipality: Int, adjustedTimeStampOption: Option[Long] = None, linkSource: LinkGeomSource) = {
    sqlu""" update asset set municipality_code = ${municipality} where id = $id """.execute
    updateAssetGeometry(id, Point(persisted.lon, persisted.lat))
    updateAssetModified(id, username).execute

    adjustedTimeStampOption match {
      case Some(adjustedTimeStamp) =>
        sqlu"""
          update lrm_position
           set
           start_measure = ${mValue},
           link_id = ${persisted.linkId},
           adjusted_timestamp = ${adjustedTimeStamp},
           link_source = ${linkSource.value}
           where id = (select position_id from asset_link where asset_id = $id)
        """.execute
      case _ =>
        sqlu"""
          update lrm_position
           set
           start_measure = ${mValue},
           link_id = ${persisted.linkId},
           link_source = ${linkSource.value}
           where id = (select position_id from asset_link where asset_id = $id)
        """.execute
    }
    id
  }

  def create(crossing: IncomingPedestrianCrossing, mValue: Double, username: String, municipality: Long, adjustedTimestamp: Long, linkSource: LinkGeomSource): Long = {
    val id = Sequences.nextPrimaryKeySeqValue
    val lrmPositionId = Sequences.nextLrmPositionPrimaryKeySeqValue
    sqlu"""
      insert all
        into asset(id, asset_type_id, created_by, created_date, municipality_code)
        values ($id, 200, $username, sysdate, ${municipality})

        into lrm_position(id, start_measure, link_id, adjusted_timestamp, link_source)
        values ($lrmPositionId, ${mValue}, ${crossing.linkId}, $adjustedTimestamp, ${linkSource.value})

        into asset_link(asset_id, position_id)
        values ($id, $lrmPositionId)
      select * from dual
    """.execute
    updateAssetGeometry(id, Point(crossing.lon, crossing.lat))

    id
  }

  def create(crossing: IncomingPedestrianCrossing, mValue: Double, username: String, municipality: Long, adjustedTimestamp: Long, linkSource: LinkGeomSource,
             createdByFromUpdate: Option[String] = Some(""),
             createdDateTimeFromUpdate: Option[DateTime]): Long = {
    val id = Sequences.nextPrimaryKeySeqValue
    val lrmPositionId = Sequences.nextLrmPositionPrimaryKeySeqValue
    sqlu"""
      insert all
        into asset(id, asset_type_id, created_by, created_date, municipality_code, modified_by, modified_date)
        values ($id, 200, $createdByFromUpdate, $createdDateTimeFromUpdate, ${municipality}, $username, sysdate)

        into lrm_position(id, start_measure, link_id, adjusted_timestamp, link_source, modified_date)
        values ($lrmPositionId, ${mValue}, ${crossing.linkId}, $adjustedTimestamp, ${linkSource.value}, sysdate)

        into asset_link(asset_id, position_id)
        values ($id, $lrmPositionId)
      select * from dual
    """.execute
    updateAssetGeometry(id, Point(crossing.lon, crossing.lat))

    id
  }

  def fetchByFilter(queryFilter: String => String): Seq[PedestrianCrossing] = {
    val queryWithFilter = queryFilter(query()) + " and (a.valid_to > sysdate or a.valid_to is null)"
    StaticQuery.queryNA[PedestrianCrossing](queryWithFilter)(getPointAssetRow).iterator.toSeq
  }

  def fetchByFilterWithExpired(queryFilter: String => String): Seq[PedestrianCrossing] = {
    val queryWithFilter = queryFilter(query())
    StaticQuery.queryNA[PedestrianCrossing](queryWithFilter)(getPointAssetRow).iterator.toSeq
  }

  private def query() = {
    """
      select a.id, pos.link_id, a.geometry, pos.start_measure, a.floating, pos.adjusted_timestamp, a.municipality_code, a.created_by, a.created_date, a.modified_by, a.modified_date,
      case when a.valid_to <= sysdate then 1 else 0 end as expired, pos.link_source
      from asset a
      join asset_link al on a.id = al.asset_id
      join lrm_position pos on al.position_id = pos.id
    """
  }


  def fetchPedestrianCrossingByLinkIds(linkIds: Seq[Long], includeExpired: Boolean = false): Seq[PedestrianCrossing] = {
    val filterExpired = if (includeExpired) "" else " and (a.valid_to > sysdate or a.valid_to is null)"
    val query =
      """
        select a.id, pos.link_id, a.geometry, pos.start_measure, a.floating, pos.adjusted_timestamp, a.municipality_code, a.created_by, a.created_date, a.modified_by, a.modified_date,
        case when a.valid_to <= sysdate then 1 else 0 end as expired, pos.link_source
        from asset a
        join asset_link al on a.id = al.asset_id
        join lrm_position pos on al.position_id = pos.id
      """
    val queryWithFilter =
      query + s"where a.asset_type_id = ${PedestrianCrossings.typeId} and pos.link_id in (${linkIds.mkString(",")})" + filterExpired
    StaticQuery.queryNA[PedestrianCrossing](queryWithFilter)(getPointAssetRow).iterator.toSeq
  }

  implicit val getPointAssetRow = new GetResult[PedestrianCrossing] {
    def apply(r: PositionedResult) : PedestrianCrossing = {
      val id = r.nextLong()
      val linkId = r.nextLong()
      val point = r.nextBytesOption().map(bytesToPoint).get
      val mValue = r.nextDouble()
      val floating = r.nextBoolean()
      val vvhTimeStamp = r.nextLong()
      val municipalityCode = r.nextInt()
      val createdBy = r.nextStringOption()
      val createdDateTime = r.nextTimestampOption().map(timestamp => new DateTime(timestamp))
      val modifiedBy = r.nextStringOption()
      val modifiedDateTime = r.nextTimestampOption().map(timestamp => new DateTime(timestamp))
      val expired = r.nextBoolean()
      val linkSource = r.nextInt()

      PedestrianCrossing(id, linkId, point.x, point.y, mValue, floating, vvhTimeStamp, municipalityCode, createdBy, createdDateTime, modifiedBy, modifiedDateTime, expired, LinkGeomSource(linkSource))
    }
  }
}
