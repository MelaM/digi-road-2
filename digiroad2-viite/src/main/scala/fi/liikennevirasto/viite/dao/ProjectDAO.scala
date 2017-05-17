package fi.liikennevirasto.viite.dao
import com.github.tototoshi.slick.MySQLJodaSupport._
import fi.liikennevirasto.digiroad2.asset.SideCode
import fi.liikennevirasto.digiroad2.masstransitstop.oracle.Sequences
import fi.liikennevirasto.digiroad2.util.Track
import fi.liikennevirasto.digiroad2.Point
import fi.liikennevirasto.viite.ReservedRoadPart
import org.joda.time.DateTime
import slick.driver.JdbcDriver.backend.Database.dynamicSession
import slick.jdbc.StaticQuery.interpolation
import slick.jdbc.{GetResult, StaticQuery => Q}

sealed trait ProjectState{
  def value: Int
  def description: String
}

object ProjectState{

  val values = Set(Closed, Incomplete)

  def apply(value: Long): ProjectState = {
    values.find(_.value == value).getOrElse(Closed)
  }

  case object Closed extends ProjectState {def value = 0; def description = "Suljettu"}
  case object Incomplete extends ProjectState { def value = 1; def description = "Keskeneräinen"}
}

sealed trait LinkStatus {
  def value: Int
}

object LinkStatus {
  val values = Set(NotHandled)
  case object NotHandled extends LinkStatus {def value = 0}
  def apply(intValue: Int): LinkStatus = {
    values.find(_.value == intValue).getOrElse(NotHandled)
  }
}

case class RoadAddressProject(id: Long, status: ProjectState, name: String, createdBy: String, createdDate: DateTime,
                              modifiedBy: String, startDate: DateTime, dateModified: DateTime, additionalInfo: String,
                              reservedParts: Seq[ReservedRoadPart])

case class ProjectLink(id: Long, roadNumber: Long, roadPartNumber: Long, track: Track,
                       discontinuity: Discontinuity, startAddrMValue: Long, endAddrMValue: Long, startDate: Option[DateTime] = None,
                       endDate: Option[DateTime] = None, modifiedBy: Option[String] = None, lrmPositionId : Long, linkId: Long, startMValue: Double, endMValue: Double, sideCode: SideCode,
                       calibrationPoints: (Option[CalibrationPoint], Option[CalibrationPoint]) = (None, None), floating: Boolean = false,
                       geom: Seq[Point], projectId: Long, status: LinkStatus) extends BaseRoadAddress

case class ProjectFormLine(startingLinkId: Long, projectId: Long, roadNumber: Long, roadPartNumber: Long, roadLength: Long, ely : Long, discontinuity: String)

object ProjectDAO {

  def create(roadAddresses: Seq[ProjectLink]): Seq[Long] = {
    val lrmPositionPS = dynamicSession.prepareStatement("insert into lrm_position (ID, link_id, SIDE_CODE, start_measure, end_measure) values (?, ?, ?, ?, ?)")
    val addressPS = dynamicSession.prepareStatement("insert into PROJECT_LINK (id, project_id, lrm_position_id, " +
      "road_number, road_part_number, " +
      "track_code, discontinuity_type, START_ADDR_M, END_ADDR_M, created_by, " +
      "calibration_points) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    val ids = sql"""SELECT lrm_position_primary_key_seq.nextval FROM dual connect by level <= ${roadAddresses.size}""".as[Long].list
    roadAddresses.zip(ids).foreach { case ((address), (lrmId)) =>
      RoadAddressDAO.createLRMPosition(lrmPositionPS, lrmId, address.linkId, address.sideCode.value, address.startMValue, address.endMValue)
      addressPS.setLong(1, if (address.id == fi.liikennevirasto.viite.NewRoadAddress) {
        Sequences.nextViitePrimaryKeySeqValue
      } else address.id)
      addressPS.setLong(2, address.projectId)
      addressPS.setLong(3, lrmId)
      addressPS.setLong(4, address.roadNumber)
      addressPS.setLong(5, address.roadPartNumber)
      addressPS.setLong(6, address.track.value)
      addressPS.setLong(7, address.discontinuity.value)
      addressPS.setLong(8, address.startAddrMValue)
      addressPS.setLong(9, address.endAddrMValue)
      addressPS.setString(10, address.modifiedBy.get)
      addressPS.setDouble(11, CalibrationCode.getFromAddress(address).value)
      addressPS.addBatch()
    }
    lrmPositionPS.executeBatch()
    addressPS.executeBatch()
    lrmPositionPS.close()
    addressPS.close()
    roadAddresses.map(_.id)
  }

  def createRoadAddressProject(roadAddressProject: RoadAddressProject): Unit = {
    sqlu"""
         insert into project (id, state, name, ely, created_by, created_date, start_date ,modified_by, modified_date, add_info)
         values (${roadAddressProject.id}, ${roadAddressProject.status.value}, ${roadAddressProject.name}, 0, ${roadAddressProject.createdBy}, sysdate ,${roadAddressProject.startDate}, '-' , sysdate, ${roadAddressProject.additionalInfo})
         """.execute
  }

  def getProjectLinks(projectId: Long): List[ProjectLink] = {
    val query =
      s"""select PROJECT_LINK.ID, PROJECT_LINK.PROJECT_ID, PROJECT_LINK.TRACK_CODE, PROJECT_LINK.DISCONTINUITY_TYPE, PROJECT_LINK.ROAD_NUMBER, PROJECT_LINK.ROAD_PART_NUMBER, PROJECT_LINK.START_ADDR_M, PROJECT_LINK.END_ADDR_M, PROJECT_LINK.LRM_POSITION_ID, PROJECT_LINK.CREATED_BY, PROJECT_LINK.MODIFIED_BY, lrm_position.link_id, (LRM_POSITION.END_MEASURE - LRM_POSITION.START_MEASURE) as length, PROJECT_LINK.CALIBRATION_POINTS, PROJECT_LINK.STATUS
                from PROJECT_LINK join LRM_POSITION
                on LRM_POSITION.ID = PROJECT_LINK.LRM_POSITION_ID
                where (PROJECT_LINK.PROJECT_ID = $projectId ) order by PROJECT_LINK.ROAD_NUMBER, PROJECT_LINK.ROAD_PART_NUMBER, PROJECT_LINK.END_ADDR_M """
    Q.queryNA[(Long, Long, Int, Int, Long, Long, Long, Long, Long, String, String, Long, Double, Long, Int)](query).list.map {
      case (projectLinkId, projectId, trackCode, discontinuityType, roadNumber, roadPartNumber, startAddrM, endAddrM, lrmPositionId, createdBy, modifiedBy, linkId, length, calibrationPoints, status) =>
        ProjectLink(projectLinkId, roadNumber, roadPartNumber, Track.apply(trackCode), Discontinuity.apply(discontinuityType), startAddrM, endAddrM, None, None, None, lrmPositionId, linkId, 0L, 0L, SideCode.apply(99), (None, None), false, Seq.empty[Point], projectId, LinkStatus.apply(status))
    }
  }

  def updateRoadAddressProject(roadAddressProject: RoadAddressProject): Unit = {
    sqlu"""
         update project set state = ${roadAddressProject.status.value}, name = ${roadAddressProject.name}, modified_by = '-' ,modified_date = sysdate where id = ${roadAddressProject.id}
         """.execute
  }

  def getRoadAddressProjectById(id: Long): Option[RoadAddressProject] = {
    val where = s""" where id =${id}"""
    val query =
      s"""SELECT id, state, name, created_by, created_date, start_date, modified_by, modified_date, add_info
          FROM project $where"""
    Q.queryNA[(Long, Long, String, String, DateTime, DateTime, String, DateTime, String)](query).list.map {
      case (id, state, name, createdBy, createdDate, start_date, modifiedBy, modifiedDate, addInfo) =>
        RoadAddressProject(id, ProjectState.apply(state), name, createdBy, start_date, modifiedBy, createdDate, modifiedDate, addInfo, List.empty[ReservedRoadPart])
    }.headOption
  }

  def getRoadAddressProjects(projectId: Long = 0): List[RoadAddressProject] = {
    val filter = projectId match {
      case 0 => ""
      case _ => s""" where id =${projectId}"""
    }
    val query =
      s"""SELECT id, state, name, created_by, created_date, start_date, modified_by, modified_date, add_info
          FROM project $filter order by name, id """
    Q.queryNA[(Long, Long, String, String, DateTime, DateTime, String, DateTime, String)](query).list.map {
      case (id, state, name, createdBy, createdDate, start_date, modifiedBy, modifiedDate, addInfo) =>
        RoadAddressProject(id, ProjectState.apply(state), name, createdBy, createdDate, modifiedBy, start_date, modifiedDate, addInfo, List.empty[ReservedRoadPart])
    }
  }

  def roadPartReservedByProject(roadNumber: Long, roadPart: Long): Option[String] = {
    val query =
      s"""SELECT p.name
              FROM project p
           INNER JOIN project_link l
           ON l.PROJECT_ID =  p.ID
           WHERE l.road_number=$roadNumber AND road_part_number=$roadPart AND rownum < 2 """
    Q.queryNA[String](query).firstOption
  }

}


