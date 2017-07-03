package fi.liikennevirasto.viite.dao

import com.github.tototoshi.slick.MySQLJodaSupport._
import fi.liikennevirasto.digiroad2.asset.LinkGeomSource.ComplimentaryLinkInterface
import fi.liikennevirasto.digiroad2.asset.{BoundingRectangle, LinkGeomSource, SideCode}
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.util.Track
import fi.liikennevirasto.digiroad2.{DigiroadEventBus, Point, RoadLinkService}
import fi.liikennevirasto.viite.dao.Discontinuity.Discontinuous
import fi.liikennevirasto.viite.{RoadAddressMerge, RoadAddressService}
import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, Matchers}
import slick.driver.JdbcDriver.backend.Database
import slick.driver.JdbcDriver.backend.Database.dynamicSession
import slick.jdbc.StaticQuery.interpolation
import slick.jdbc.{StaticQuery => Q}


/**
  * Created by venholat on 12.9.2016.
  */
class RoadAddressDAOSpec extends FunSuite with Matchers {

  def runWithRollback(f: => Unit): Unit = {
    Database.forDataSource(OracleDatabase.ds).withDynTransaction {
      f
      dynamicSession.rollback()
    }
  }

  test("testFetchByRoadPart") {
    runWithRollback {
      RoadAddressDAO.fetchByRoadPart(5L, 201L).isEmpty should be(false)
    }
  }

  test("testFetchByLinkId") {
    runWithRollback {
      val sets = RoadAddressDAO.fetchByLinkId(Set(5170942, 5170947))
      sets.size should be (2)
      sets.forall(_.floating == false) should be (true)
    }
  }

  test("Get valid road numbers") {
    runWithRollback {
      val numbers = RoadAddressDAO.getCurrentValidRoadNumbers()
      numbers.isEmpty should be(false)
      numbers should contain(5L)
    }
  }

  test("Get valid road part numbers") {
    runWithRollback {
      val numbers = RoadAddressDAO.getValidRoadParts(5L)
      numbers.isEmpty should be(false)
      numbers should contain(201L)
    }
  }

  test("Update without geometry") {
    runWithRollback {
      val address = RoadAddressDAO.fetchByLinkId(Set(5170942)).head
      RoadAddressDAO.update(address)
    }
  }

  test("Updating a geometry is executed in SQL server") {
    runWithRollback {
      val address = RoadAddressDAO.fetchByLinkId(Set(5170942)).head
      RoadAddressDAO.update(address, Some(Seq(Point(50200, 7630000.0, 0.0), Point(50210, 7630000.0, 10.0))))
      RoadAddressDAO.fetchByBoundingBox(BoundingRectangle(Point(50202, 7620000), Point(50205, 7640000)), false).
        _1.exists(_.id == address.id) should be (true)
      RoadAddressDAO.fetchByBoundingBox(BoundingRectangle(Point(50212, 7620000), Point(50215, 7640000)), false).
        _1.exists(_.id == address.id) should be (false)
    }
  }


  test("Set road address to floating and update the geometry as well") {
    runWithRollback {
      val address = RoadAddressDAO.fetchByLinkId(Set(5170942)).head
      RoadAddressDAO.changeRoadAddressFloating(true, address.id, Some(Seq(Point(50200, 7630000.0, 0.0), Point(50210, 7630000.0, 10.0))))
    }
  }

  test("Create Road Address") {
    runWithRollback {
      val id = RoadAddressDAO.getNextRoadAddressId
      val ra = Seq(RoadAddress(id, 1943845, 1, Track.Combined, Discontinuous, 0L, 10L, Some(DateTime.parse("1901-01-01")), None, Option("tester"), 0, 12345L, 0.0, 9.8, SideCode.TowardsDigitizing, 0, (None, None), false,
        Seq(Point(0.0, 0.0), Point(0.0, 9.8)), LinkGeomSource.NormalLinkInterface))
      val currentSize = RoadAddressDAO.fetchByRoadPart(ra.head.roadNumber, ra.head.roadPartNumber).size
      val returning = RoadAddressDAO.create(ra)
      returning.nonEmpty should be (true)
      returning.head should be (id)
      val newSize = currentSize + 1
      RoadAddressDAO.fetchByRoadPart(ra.head.roadNumber, ra.head.roadPartNumber) should have size(newSize)
    }
  }

  test("Adding geometry to missing roadaddress") {
    runWithRollback {
      val id = RoadAddressDAO.getNextRoadAddressId
      sqlu"""
           insert into missing_road_address (link_id, start_addr_m, end_addr_m,anomaly_code, start_m)
           values ($id, 0, 1, 1, 1)
           """.execute
      sqlu"""UPDATE MISSING_ROAD_ADDRESS
        SET geometry= MDSYS.SDO_GEOMETRY(4002, 3067, NULL, MDSYS.SDO_ELEM_INFO_ARRAY(1,2,1), MDSYS.SDO_ORDINATE_ARRAY(
             6699381,396898,0,0.0,6699382,396898,0,2))
        WHERE link_id = ${id}""".execute
      val query= s"""select Count(geometry)
                 from missing_road_address ra
                 WHERE ra.link_id=$id AND geometry IS NOT NULL
      """
      Q.queryNA[Int](query).firstOption should be (Some(1))
    }
  }

  test("Create Road Address with username") {
    runWithRollback {
      val username = "testUser"
      val id = RoadAddressDAO.getNextRoadAddressId
      val ra = Seq(RoadAddress(id, 1943845, 1, Track.Combined, Discontinuous, 0L, 10L, Some(DateTime.parse("1901-01-01")), None, Option("tester"), 0, 12345L, 0.0, 9.8, SideCode.TowardsDigitizing, 0, (None, None), false,
        Seq(Point(0.0, 0.0), Point(0.0, 9.8)), LinkGeomSource.NormalLinkInterface))
      val currentSize = RoadAddressDAO.fetchByRoadPart(ra.head.roadNumber, ra.head.roadPartNumber).size
      val returning = RoadAddressDAO.create(ra, Some(username))
      returning.nonEmpty should be (true)
      returning.head should be (id)
      val newSize = currentSize + 1
      val roadAddress = RoadAddressDAO.fetchByRoadPart(ra.head.roadNumber, ra.head.roadPartNumber)
      roadAddress should have size(newSize)
      roadAddress.head.modifiedBy.get should be (username)
    }
  }

  test("Create Road Address With Calibration Point") {
    runWithRollback {
      val id = RoadAddressDAO.getNextRoadAddressId
      val ra = Seq(RoadAddress(id, 1943845, 1, Track.Combined, Discontinuous, 0L, 10L, Some(DateTime.parse("1901-01-01")), None, Option("tester"),0, 12345L, 0.0, 9.8, SideCode.TowardsDigitizing, 0,
        (Some(CalibrationPoint(12345L, 0.0, 0L)), None), false,
        Seq(Point(0.0, 0.0), Point(0.0, 9.8)), LinkGeomSource.NormalLinkInterface))
      val returning = RoadAddressDAO.create(ra)
      returning.nonEmpty should be (true)
      returning.head should be (id)
      val fetch = sql"""select calibration_points from road_address where id = $id""".as[Int].list
      fetch.head should be (2)
    }
    runWithRollback {
      val id = RoadAddressDAO.getNextRoadAddressId
      val ra = Seq(RoadAddress(id, 1943845, 1, Track.Combined, Discontinuous, 0L, 10L, Some(DateTime.parse("1901-01-01")), None, Option("tester"),0, 12345L, 0.0, 9.8, SideCode.TowardsDigitizing, 0,
        (Some(CalibrationPoint(12345L, 0.0, 0L)), Some(CalibrationPoint(12345L, 9.8, 10L))), false,
        Seq(Point(0.0, 0.0), Point(0.0, 9.8)), LinkGeomSource.NormalLinkInterface))
      val returning = RoadAddressDAO.create(ra)
      returning.nonEmpty should be (true)
      returning.head should be (id)
      val fetch = sql"""select calibration_points from road_address where id = $id""".as[Int].list
      fetch.head should be (3)
    }
  }

  test("Create Road Address with complementary source") {
    runWithRollback {
      val id = RoadAddressDAO.getNextRoadAddressId
      val ra = Seq(RoadAddress(id, 1943845, 1, Track.Combined, Discontinuous, 0L, 10L, Some(DateTime.parse("1901-01-01")),
        None, Option("tester"), 0, 12345L, 0.0, 9.8, SideCode.TowardsDigitizing, 0, (None, None), false,
        Seq(Point(0.0, 0.0), Point(0.0, 9.8)), LinkGeomSource.ComplimentaryLinkInterface))
      val returning = RoadAddressDAO.create(ra)
      returning.nonEmpty should be (true)
      returning.head should be (id)
      sql"""SELECT link_source FROM ROAD_ADDRESS ra JOIN LRM_POSITION pos ON (ra.lrm_position_id = pos.id) WHERE ra.id = $id"""
        .as[Int].first should be (ComplimentaryLinkInterface.value)
    }
  }


  test("Delete Road Addresses") {
    runWithRollback {
      val addresses = RoadAddressDAO.fetchByRoadPart(5, 206)
      addresses.nonEmpty should be (true)
      RoadAddressDAO.remove(addresses) should be (addresses.size)
      sql"""SELECT COUNT(*) FROM ROAD_ADDRESS WHERE ROAD_NUMBER = 5 AND ROAD_PART_NUMBER = 206 AND VALID_TO IS NULL""".as[Long].first should be (0L)
    }
  }

  test("test update for merged Road Addresses") {
    val localMockRoadLinkService = MockitoSugar.mock[RoadLinkService]
    val localMockEventBus = MockitoSugar.mock[DigiroadEventBus]
    val localRoadAddressService = new RoadAddressService(localMockRoadLinkService,localMockEventBus)
    runWithRollback {
      val id = RoadAddressDAO.getNextRoadAddressId
      val toBeMergedRoadAddresses = Seq(RoadAddress(id, 1943845, 1, Track.Combined, Discontinuous, 0L, 10L, Some(DateTime.parse("1901-01-01")), None, Option("tester"),0, 6556558L, 0.0, 9.8, SideCode.TowardsDigitizing, 0, (None, None), false,
        Seq(Point(0.0, 0.0), Point(0.0, 9.8)), LinkGeomSource.NormalLinkInterface))
      localRoadAddressService.mergeRoadAddressInTX(RoadAddressMerge(Set(1L), toBeMergedRoadAddresses))
    }
  }

  ignore("test if road addresses are expired") {
    def now(): DateTime = {
      OracleDatabase.withDynSession {
        return sql"""select sysdate FROM dual""".as[DateTime].list.head
      }
    }

    val beforeCallMethodDatetime = now()
    runWithRollback {
      val linkIds: Set[Long] = Set(4147081)
      RoadAddressDAO.expireRoadAddresses(linkIds)
      val dbResult = sql"""select valid_to FROM road_address where lrm_position_id in (select id from lrm_position where link_id in(4147081))""".as[DateTime].list
      dbResult.size should be (1)
      dbResult.foreach{ date =>
        date.getMillis should be >= beforeCallMethodDatetime.getMillis
      }
    }
  }

  test("find road address by start or end address value") {
    OracleDatabase.withDynSession {
      val s = RoadAddressDAO.fetchByAddressStart(75, 1, Track.apply(2), 875)
      val e = RoadAddressDAO.fetchByAddressEnd(75, 1, Track.apply(2), 995)
      s.isEmpty should be(false)
      e.isEmpty should be(false)
      s should be(e)
    }
  }

}