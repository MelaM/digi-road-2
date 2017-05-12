package fi.liikennevirasto.digiroad2.pointasset.oracle

import fi.liikennevirasto.digiroad2.{Point, PersistedPointAsset}
import fi.liikennevirasto.digiroad2.masstransitstop.oracle.Queries._
import fi.liikennevirasto.digiroad2.masstransitstop.oracle.Sequences
import org.joda.time.DateTime
import slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import slick.jdbc.{GetResult, PositionedResult, StaticQuery}
import slick.jdbc.StaticQuery.interpolation

case class TrafficLight(id: Long, linkId: Long,
                              lon: Double, lat: Double,
                              mValue: Double, floating: Boolean,
                              vvhTimeStamp: Long,
                              municipalityCode: Int,
                              createdBy: Option[String] = None,
                              createdAt: Option[DateTime] = None,
                              modifiedBy: Option[String] = None,
                              modifiedAt: Option[DateTime] = None) extends PersistedPointAsset

case class TrafficLightToBePersisted(linkId: Long, lon: Double, lat: Double, mValue: Double, municipalityCode: Int, createdBy: String)

object OracleTrafficLightDao {
  def fetchByFilter(queryFilter: String => String): Seq[TrafficLight] = {

    val query =
      """
        select a.id, pos.link_id, a.geometry, pos.start_measure, a.floating, pos.adjusted_timestamp, a.municipality_code, a.created_by, a.created_date, a.modified_by, a.modified_date
        from asset a
        join asset_link al on a.id = al.asset_id
        join lrm_position pos on al.position_id = pos.id
      """
    val queryWithFilter = queryFilter(query) + " and (a.valid_to > sysdate or a.valid_to is null)"
    StaticQuery.queryNA[TrafficLight](queryWithFilter).iterator.toSeq
  }

  implicit val getPointAsset = new GetResult[TrafficLight] {
    def apply(r: PositionedResult) = {
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

      TrafficLight(id, linkId, point.x, point.y, mValue, floating, vvhTimeStamp, municipalityCode, createdBy, createdDateTime, modifiedBy, modifiedDateTime)
    }
  }

  def create(trafficLight: TrafficLightToBePersisted, username: String, adjustedTimestamp: Long): Long = {
    val id = Sequences.nextPrimaryKeySeqValue
    val lrmPositionId = Sequences.nextLrmPositionPrimaryKeySeqValue
    sqlu"""
      insert all
        into asset(id, asset_type_id, created_by, created_date, municipality_code)
        values ($id, 280, $username, sysdate, ${trafficLight.municipalityCode})
        into lrm_position(id, start_measure, link_id, adjusted_timestamp)
        values ($lrmPositionId, ${trafficLight.mValue}, ${trafficLight.linkId}, $adjustedTimestamp)

        into asset_link(asset_id, position_id)
        values ($id, $lrmPositionId)
      select * from dual
    """.execute
    updateAssetGeometry(id, Point(trafficLight.lon, trafficLight.lat))

    id
  }

  def update(id: Long, trafficLight: TrafficLightToBePersisted, adjustedTimeStampOption: Option[Long] = None) = {
    sqlu""" update asset set municipality_code = ${trafficLight.municipalityCode} where id = $id """.execute
    updateAssetGeometry(id, Point(trafficLight.lon, trafficLight.lat))
    updateAssetModified(id, trafficLight.createdBy).execute

    adjustedTimeStampOption match {
      case Some(adjustedTimeStamp) =>
        sqlu"""
          update lrm_position
           set
           start_measure = ${trafficLight.mValue},
           link_id = ${trafficLight.linkId},
           adjusted_timestamp = ${adjustedTimeStamp}
           where id = (select position_id from asset_link where asset_id = $id)
        """.execute
      case _  =>
        sqlu"""
          update lrm_position
           set
           start_measure = ${trafficLight.mValue},
           link_id = ${trafficLight.linkId}
           where id = (select position_id from asset_link where asset_id = $id)
        """.execute
    }
    id
  }
}