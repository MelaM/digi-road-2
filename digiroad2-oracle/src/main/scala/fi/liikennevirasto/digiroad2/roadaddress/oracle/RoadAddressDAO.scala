package fi.liikennevirasto.digiroad2.roadaddress.oracle

import slick.jdbc.StaticQuery
import org.joda.time.DateTime
import fi.liikennevirasto.digiroad2.Point
import slick.jdbc.{GetResult, StaticQuery => Q}
import com.github.tototoshi.slick.MySQLJodaSupport._
import fi.liikennevirasto.digiroad2.asset.SideCode
import fi.liikennevirasto.digiroad2.util.Track
import slick.driver.JdbcDriver.backend.Database.dynamicSession
import slick.jdbc.StaticQuery.interpolation


case class RoadAddress(id: Long, roadNumber: Long, roadPartNumber: Long, track: Track, discontinuity: Int, startAddrMValue: Long, endAddrMValue: Long, startDate: Option[DateTime] = None,
                       endDate: Option[DateTime] = None, lrmPositionId: Long, linkId: Long,
                       startMValue: Double, endMValue: Double, sideCode: SideCode, floating: Boolean = false, geom: Seq[Point])

class RoadAddressDAO {

  def getRoadAddress(queryFilter: String => String): Seq[RoadAddress] = {
    val query =
      s"""
           select ra.id, ra.road_number, ra.road_part_number, ra.track_code, ra.discontinuity, ra.start_addr_m, ra.end_addr_m, ra.start_date, ra.end_date,
           ra.lrm_position_id, pos.link_id, pos.start_measure, pos.end_measure, pos.side_code,
           ra.floating,
           (SELECT X FROM TABLE(SDO_UTIL.GETVERTICES(ra.geometry)) t WHERE id = 1) as X,
           (SELECT Y FROM TABLE(SDO_UTIL.GETVERTICES(ra.geometry)) t WHERE id = 1) as Y,
           (SELECT X FROM TABLE(SDO_UTIL.GETVERTICES(ra.geometry)) t WHERE id = 2) as X2,
           (SELECT Y FROM TABLE(SDO_UTIL.GETVERTICES(ra.geometry)) t WHERE id = 2) as Y2
           from road_address ra
           join lrm_position pos on ra.lrm_position_id = pos.id"""

    queryList(queryFilter(query) + " and (ra.valid_to > sysdate or ra.valid_to is null) ")
  }

  def withRoadAddress(road: Long, roadPart: Long, track: Int, mValue: Double)(query: String): String = {
    query + s" WHERE ra.road_number = $road AND ra.road_part_number = $roadPart " +
      s"  AND ra.track_code = $track AND ra.start_addr_M <= $mValue AND ra.end_addr_M > $mValue"
  }

  def withLinkIdAndMeasure(linkId: Long, startM: Long, endM: Long, road: Option[Int] = None)(query: String): String = {

    val qfilter = (road) match {
      case Some(road) => "AND road_number = " + road
      case (_) => " "
    }
    query + s" WHERE pos.link_id = $linkId AND pos.start_Measure <= $startM AND pos.end_Measure > $endM " + qfilter
  }

  def getRoadNumbers(): Seq[Long] = {
    sql"""
			select distinct (ra.road_number)
      from road_address ra
      where ra.valid_to is null OR ra.valid_to > SYSDATE
		  """.as[Long].list
  }

  def getRoadAddressesFiltered(roadNumber: Long, roadPartNumber: Long, startM: Double, endM: Double): Seq[RoadAddress] = {
    val where =
      s""" where (( ra.start_addr_m >= $startM and ra.end_addr_m <= $endM ) or ( $startM >= ra.start_addr_m and $startM < ra.end_addr_m) or
         ( $endM > ra.start_addr_m and $endM <= ra.end_addr_m)) and ra.road_number= $roadNumber and ra.road_part_number= $roadPartNumber
         and (valid_to is null OR valid_to > SYSDATE) and ra.floating = 0 """

    val query =
      s"""
			select distinct ra.id, ra.road_number, ra.road_part_number, ra.track_code,
      ra.discontinuity, ra.start_addr_m, ra.end_addr_m, ra.start_date, ra.end_date, ra.lrm_position_id, pos.link_id,
      pos.start_measure, pos.end_measure, pos.side_code, ra.floating, ra.valid_to
      from road_address ra
      join lrm_position pos on ra.lrm_position_id = pos.id
      $where
      """
    queryRoadAddresses(query)
  }

  def getByLinkIdAndMeasures(linkId: Long, startM: Double, endM: Double): List[RoadAddress] = {

    val where =
      s""" where pos.link_id = $linkId and
         (( pos.start_measure >= $startM and pos.end_measure <= $endM ) or
         ( $endM >= pos.start_measure and $endM <= pos.end_measure)) """

    val query =
      s"""
			 select ra.id, ra.road_number, ra.road_part_number, ra.track_code,
       ra.discontinuity, ra.start_addr_m, ra.end_addr_m, ra.start_date, ra.end_date, ra.lrm_position_id, pos.link_id, pos.start_measure, pos.end_measure,
       pos.side_code, ra.floating, t.X, t.Y, t2.X, t2.Y
       from road_address ra cross join
       TABLE(SDO_UTIL.GETVERTICES(ra.geometry)) t cross join
       TABLE(SDO_UTIL.GETVERTICES(ra.geometry)) t2
       join lrm_position pos on ra.lrm_position_id = pos.id
			 $where
		  """

    queryList(query)
  }

  implicit val getTrack = GetResult[Track](r => Track.apply(r.nextInt()))

  private def queryList(query: String) = {
    val tuples = Q.queryNA[(Long, Long, Long, Int, Int, Long, Long, Option[DateTime],
      Option[DateTime], Long, Long, Double, Double, Int,
      Boolean, Double, Double, Double, Double)](query).list

    tuples.map {
      case (id, roadNumber, roadPartNumber, track, discontinuity, startAddrMValue, endAddrMValue, startDate, endDate, lrmPositionId,
      linkId, startMValue, endMValue, sideCode, floating, x, y, x2, y2) =>

        RoadAddress(id, roadNumber, roadPartNumber, Track.apply(track), discontinuity,
          startAddrMValue, endAddrMValue, startDate, endDate, lrmPositionId, linkId, startMValue, endMValue, SideCode.apply(sideCode),
          floating, Seq(Point(x, y), Point(x2, y2)))
    }
  }

  private def queryRoadAddresses(query: String) = {
    val tuples = Q.queryNA[(Long, Long, Long, Int, Int, Long, Long, Option[DateTime],
      Option[DateTime], Long, Long, Double, Double, Int,
      Boolean)](query).list

    tuples.map {
      case (id, roadNumber, roadPartNumber, track, discontinuity, startAddrMValue, endAddrMValue, startDate, endDate, lrmPositionId,
      linkId, startMValue, endMValue, sideCode, floating) =>

        RoadAddress(id, roadNumber, roadPartNumber, Track.apply(track), discontinuity,
          startAddrMValue, endAddrMValue, startDate, endDate, lrmPositionId, linkId, startMValue, endMValue, SideCode.apply(sideCode),
          floating, Seq())
    }
  }

}
