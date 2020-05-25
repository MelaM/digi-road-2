package fi.liikennevirasto.digiroad2.service.lane

import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.client.vvh.{VVHClient, VVHRoadLinkClient}
import fi.liikennevirasto.digiroad2.dao.{MunicipalityDao, RoadAddressTEMP}
import fi.liikennevirasto.digiroad2.dao.lane.LaneDao
import fi.liikennevirasto.digiroad2.lane.LaneFiller.ChangeSet
import fi.liikennevirasto.digiroad2.lane.{LaneProperty, LanePropertyValue, NewIncomeLane, PersistedLane}
import fi.liikennevirasto.digiroad2.linearasset.RoadLink
import fi.liikennevirasto.digiroad2.service.RoadLinkService
import fi.liikennevirasto.digiroad2.util.{PolygonTools, TestTransactions, Track}
import fi.liikennevirasto.digiroad2.{DigiroadEventBus, Point}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}


class LaneTestSupporter extends FunSuite with Matchers {
  val mockRoadLinkService = MockitoSugar.mock[RoadLinkService]
  val mockVVHClient = MockitoSugar.mock[VVHClient]
  val mockVVHRoadLinkClient = MockitoSugar.mock[VVHRoadLinkClient]
  val mockPolygonTools = MockitoSugar.mock[PolygonTools]
  val mockEventBus = MockitoSugar.mock[DigiroadEventBus]
  val mockMunicipalityDao = MockitoSugar.mock[MunicipalityDao]
  val mockLaneDao = MockitoSugar.mock[LaneDao]

  val laneDao = new LaneDao(mockVVHClient, mockRoadLinkService)
  val roadLinkWithLinkSource = RoadLink(
    1, Seq(Point(0.0, 0.0), Point(10.0, 0.0)), 10.0, Municipality,
    1, TrafficDirection.BothDirections, Motorway, None, None, Map("MUNICIPALITYCODE" -> BigInt(235), "SURFACETYPE" -> BigInt(2)), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)


  when(mockRoadLinkService.getRoadLinkByLinkIdFromVVH(any[Long], any[Boolean])).thenReturn(Some(roadLinkWithLinkSource))
  when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)


  val lanePropertiesValues11 = Seq( LaneProperty("lane_code", Seq(LanePropertyValue(11))),
                                LaneProperty("lane_continuity", Seq(LanePropertyValue("1"))),
                                LaneProperty("lane_type", Seq(LanePropertyValue("2"))),
                                LaneProperty("lane_information", Seq(LanePropertyValue("13")))
                              )


  val lanePropertiesValues12 = Seq( LaneProperty("lane_code", Seq(LanePropertyValue(12))),
                                LaneProperty("lane_continuity", Seq(LanePropertyValue("1"))),
                                LaneProperty("lane_type", Seq(LanePropertyValue("2"))),
                                LaneProperty("lane_information", Seq(LanePropertyValue("13")))
                              )

  val lanePropertiesValues14 = Seq(LaneProperty("lane_code", Seq(LanePropertyValue(14))),
                                   LaneProperty("lane_continuity", Seq(LanePropertyValue("2"))),
                                   LaneProperty("lane_type", Seq(LanePropertyValue("3"))),
                                   LaneProperty("lane_information", Seq(LanePropertyValue("sub lane 14")))
                                  )


  val lanePropertiesValues21 = Seq( LaneProperty("lane_code", Seq(LanePropertyValue(21))),
                                LaneProperty("lane_continuity", Seq(LanePropertyValue("1"))),
                                LaneProperty("lane_type", Seq(LanePropertyValue("2"))),
                                LaneProperty("lane_information", Seq(LanePropertyValue("10")))
                              )


  val lanePropertiesValues22 = Seq( LaneProperty("lane_code", Seq(LanePropertyValue(22))),
                                LaneProperty("lane_continuity", Seq(LanePropertyValue("1"))),
                                LaneProperty("lane_type", Seq(LanePropertyValue("2"))),
                                LaneProperty("lane_information", Seq(LanePropertyValue("10")))
                              )



  object PassThroughLaneService extends LaneOperations {
    override def withDynTransaction[T](f: => T): T = f
    override def roadLinkService: RoadLinkService = mockRoadLinkService
    override def dao: LaneDao = mockLaneDao
    override def eventBus: DigiroadEventBus = mockEventBus
    override def vvhClient: VVHClient = mockVVHClient
    override def polygonTools: PolygonTools = mockPolygonTools
    override def municipalityDao: MunicipalityDao = mockMunicipalityDao

  }

  def runWithRollback(test: => Unit): Unit = TestTransactions.runWithRollback(PassThroughLaneService.dataSource)(test)
}


class LaneServiceSpec extends LaneTestSupporter {

  object ServiceWithDao extends LaneService(mockRoadLinkService, mockEventBus) {
    
    override def withDynTransaction[T](f: => T): T = f
    override def roadLinkService: RoadLinkService = mockRoadLinkService
    override def vvhClient: VVHClient = mockVVHClient
    override def dao: LaneDao = laneDao
    override def municipalityDao: MunicipalityDao = mockMunicipalityDao
    override def eventBus: DigiroadEventBus = mockEventBus
    override def polygonTools: PolygonTools = mockPolygonTools

  }

  val usernameTest = "testuser"

  test("Create new lane") {
    runWithRollback {

      val newLane = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)), Set(100L), 1, usernameTest)
      newLane.length should be(1)

      val lane = laneDao.fetchLanesByIds( Set(newLane.head)).head
      lane.expired should be (false)

      lane.attributes.foreach{ laneProp =>
        val attr = lanePropertiesValues11.find(_.publicId == laneProp.publicId)

        attr should not be (None)
        attr.head.values.head.value should be  (laneProp.values.head.value)
      }

    }

  }

  test("Should not be able to delete main lanes") {
    runWithRollback {
      val newLane = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)), Set(100L), 1, usernameTest)
      newLane.length should be(1)

      val thrown = intercept[IllegalArgumentException] {
        ServiceWithDao.deleteMultipleLanes(newLane.toSet)
      }

      thrown.getMessage should be("Cannot Delete a main lane!")

    }
  }

  test("Fetch existing main lanes by linkId"){
    runWithRollback {
      val newLane11 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)), Set(100L), 1, usernameTest)
      newLane11.length should be(1)

      val newLane12 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues12)), Set(100L), 1, usernameTest)
      newLane12.length should be(1)

      val newLane21 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues21)), Set(100L), 2, usernameTest)
      newLane21.length should be(1)

      val mockRoadLink = RoadLink(100L, Seq(), 1000, State, 2, TrafficDirection.AgainstDigitizing,
        EnclosedTrafficArea, None, None)

      val existingLanes = ServiceWithDao.fetchExistingMainLanesByRoadLinks(Seq(mockRoadLink), Seq())
      existingLanes.length should be(2)

    }
  }

  test("Fetch existing Lanes by linksId"){
    runWithRollback {

      val newLane11 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)), Set(100L), 1, usernameTest)
      newLane11.length should be(1)

      val newLane21 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues21)), Set(100L), 2, usernameTest)
      newLane21.length should be(1)

      val newLane22 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues22)), Set(100L), 2, usernameTest)
      newLane22.length should be(1)

      val existingLanes = ServiceWithDao.fetchExistingLanesByLinksIdAndSideCode(100L, 1)
      existingLanes.length should be(1)

      val existingLanesOtherSide = ServiceWithDao.fetchExistingLanesByLinksIdAndSideCode(100L, 2)
      existingLanesOtherSide.length should be(2)

    }
  }


  test("Update Lane Information") {
    runWithRollback {

      val updateValues11 = Seq( LaneProperty("lane_code", Seq(LanePropertyValue(11))),
                            LaneProperty("lane_continuity", Seq(LanePropertyValue("2"))),
                            LaneProperty("lane_type", Seq(LanePropertyValue("5"))),
                            LaneProperty("lane_information", Seq(LanePropertyValue("12")))
                          )


      val newLane11 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)), Set(100L), 1, usernameTest)
      newLane11.length should be(1)

      val updatedLane = ServiceWithDao.update(Seq(NewIncomeLane(newLane11.head, 0, 500, 745, false, false, updateValues11)), Set(100L), 1, usernameTest)
      updatedLane.length should be(1)

      }
  }

  test("Should not be able to Update main Lane") {
    runWithRollback {

      val updateValues11 = Seq( LaneProperty("lane_code", Seq(LanePropertyValue(12))),
                            LaneProperty("lane_continuity", Seq(LanePropertyValue("2"))),
                            LaneProperty("lane_type", Seq(LanePropertyValue("5"))),
                            LaneProperty("lane_information", Seq(LanePropertyValue("12")))
                          )


      val newLane11 = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)), Set(100L), 1, usernameTest)
      newLane11.length should be(1)

      val thrown = intercept[IllegalArgumentException] {
        ServiceWithDao.update(Seq(NewIncomeLane(newLane11.head, 0, 500, 745, false, false, updateValues11)), Set(100L), 1, usernameTest)
      }

      thrown.getMessage should be("Cannot change the code of main lane!")

    }
  }

  test("Expire a sub lane") {
    runWithRollback {
      val newSubLaneId = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues12)), Set(100L), 1, usernameTest).head
      ServiceWithDao.multipleLanesToHistory(Set(newSubLaneId), usernameTest)

      val lanes = laneDao.fetchLanesByLinkIdsAndLaneCode(Seq(100L), Seq(12), true)

      val expiredLane = lanes.find(_.id == newSubLaneId).head
      lanes.find(_.id == newSubLaneId).head.expired should be (true)
      expiredLane.attributes.foreach{ laneProp =>
        lanePropertiesValues12.contains(laneProp) should be(true)
      }
      expiredLane.startMeasure should be (0.0)
      expiredLane.endMeasure should be (500.0)
    }
  }

  test("Delete a sub lane") {
    runWithRollback {
      val newSubLaneId = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues12)), Set(100L), 1, usernameTest).head
      ServiceWithDao.deleteMultipleLanes(Set(newSubLaneId))

      val lanes = laneDao.fetchLanesByLinkIdsAndLaneCode(Seq(100L), Seq(12), true)
      lanes.find(_.id == newSubLaneId) should be(None)
    }
  }

  test("Split lane asset. Expire lane and create two new lanes") {
    runWithRollback {
      val lanePropertiesSubLaneSplit2 = Seq(
        LaneProperty("lane_code", Seq(LanePropertyValue(12))),
        LaneProperty("lane_continuity", Seq(LanePropertyValue("4"))),
        LaneProperty("lane_type", Seq(LanePropertyValue("5"))),
        LaneProperty("lane_information", Seq(LanePropertyValue("splitted 2")))
      )

      val mainLane = NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)
      val mainLaneId = ServiceWithDao.create(Seq(mainLane), Set(100L), 1, usernameTest).head
      val newSubLaneId = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues12)), Set(100L), 1, usernameTest).head

      val subLaneSplit1 = NewIncomeLane(0, 0, 250, 745, false, false, lanePropertiesValues12)
      val subLaneSplit2 = NewIncomeLane(0, 250, 500, 745, false, false, lanePropertiesSubLaneSplit2)
      ServiceWithDao.update(Seq(mainLane.copy(id = mainLaneId)), Set(100L), 1, usernameTest)
      ServiceWithDao.multipleLanesToHistory(Set(newSubLaneId), usernameTest)

      val subLaneSplit1Id = ServiceWithDao.create(Seq(subLaneSplit1), Set(100L), 1, usernameTest).head
      val subLaneSplit2Id = ServiceWithDao.create(Seq(subLaneSplit2), Set(100L), 1, usernameTest).head

      val lanes = laneDao.fetchLanesByLinkIdsAndLaneCode(Seq(100L), Seq(11, 12), true)

      lanes.find(_.id == newSubLaneId).head.expired should be (true)

      val split1 = lanes.find(_.id == subLaneSplit1Id).head
      split1.attributes.foreach{ laneProp =>
       lanePropertiesValues12.contains(laneProp) should be(true)
      }
      split1.startMeasure should be (0.0)
      split1.endMeasure should be (250.0)

      val split2 = lanes.find(_.id == subLaneSplit2Id).head
      split2.attributes.foreach{ laneProp =>
        lanePropertiesSubLaneSplit2.contains(laneProp) should be(true)
      }
      split2.startMeasure should be (250.0)
      split2.endMeasure should be (500.0)
    }
  }

  test("Delete sub lane in middle of lanes") {
    runWithRollback {
      val lanePropertiesValues14To12 = Seq(LaneProperty("lane_code", Seq(LanePropertyValue(12))),
        LaneProperty("lane_continuity", Seq(LanePropertyValue("2"))),
        LaneProperty("lane_type", Seq(LanePropertyValue("3"))),
        LaneProperty("lane_information", Seq(LanePropertyValue("updated sub lane 14 to 12")))
      )

      val mainLane = NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues11)
      val subLane14 = NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues14)

      val mainLaneId = ServiceWithDao.create(Seq(mainLane), Set(100L), 1, usernameTest).head
      val newSubLane12Id = ServiceWithDao.create(Seq(NewIncomeLane(0, 0, 500, 745, false, false, lanePropertiesValues12)), Set(100L), 1, usernameTest).head
      val newSubLane14Id = ServiceWithDao.create(Seq(subLane14), Set(100L), 1, usernameTest).head

      ServiceWithDao.deleteMultipleLanes(Set(newSubLane12Id))
      ServiceWithDao.update(Seq(mainLane.copy(id = mainLaneId), subLane14.copy(id = newSubLane14Id, properties = lanePropertiesValues14To12)), Set(100L), 1, usernameTest)

      val lanes = laneDao.fetchLanesByLinkIdsAndLaneCode(Seq(100L), Seq(11, 12), true)
      lanes.length should be (2)
      lanes.find(_.id == newSubLane12Id) should be (None)

      val updatedSubLane12 = lanes.find(_.id == newSubLane14Id).head
      updatedSubLane12.attributes.foreach{ laneProp =>
        lanePropertiesValues14To12.contains(laneProp) should be(true)
      }
      updatedSubLane12.startMeasure should be (0.0)
      updatedSubLane12.endMeasure should be (500.0)
    }
  }


  test("RoadLink BothDirection and with only one Lane (TowardsDigitizing). The 2nd should be created.") {
    runWithRollback {

      val roadLinkTowards2 = RoadLink(2L, Seq(Point(0.0, 0.0), Point(15.0, 0.0)), 15.0, Municipality,
                              1, TrafficDirection.BothDirections, Motorway, None, None)

      val lane11 = PersistedLane(2L, roadLinkTowards2.linkId, SideCode.TowardsDigitizing.value,
                  11, 745L, 0, 15.0, None, None, None, None, expired = false, 0L, None,
                  Seq(LaneProperty("lane_code", Seq(LanePropertyValue(11)))) )


      val roadAddress = RoadAddressTEMP(roadLinkTowards2.linkId, 6, 202, Track.Combined, 1, 150, 0, 150,
                        Seq(Point(0.0, 0.0), Point(15.0, 0.0)), Some(SideCode.TowardsDigitizing), None)

      val (persistedLanes, changeSet) = ServiceWithDao.adjustLanesSideCodes(roadLinkTowards2, Seq(lane11), ChangeSet(), Seq(roadAddress) )

      persistedLanes should have size 2
      persistedLanes.map(_.sideCode) should be(Seq(SideCode.TowardsDigitizing.value, SideCode.AgainstDigitizing.value))
      persistedLanes.map(_.id) should be(Seq(2L, 0L))
      persistedLanes.map(_.linkId) should be(Seq(roadLinkTowards2.linkId, roadLinkTowards2.linkId))

      changeSet.expiredLaneIds should be(Set())

    }
  }

  test("RoadLink BothDirection and with only one Lane (AgainstDigitizing). The 2nd should be created.") {

    val roadLinkTowards2 = RoadLink(2L, Seq(Point(0.0, 0.0), Point(15.0, 0.0)), 15.0, Municipality,
                            1, TrafficDirection.BothDirections, Motorway, None, None)

    val lane11 = PersistedLane(2L, roadLinkTowards2.linkId, SideCode.AgainstDigitizing.value,
                  21, 745L, 0, 15.0, None, None, None, None, expired = false, 0L, None,
                  Seq(LaneProperty("lane_code", Seq(LanePropertyValue(21)))) )

    val roadAddress = RoadAddressTEMP(roadLinkTowards2.linkId, 6, 202, Track.Combined, 1, 150, 0, 150,
                        Seq(Point(0.0, 0.0), Point(15.0, 0.0)), Some(SideCode.TowardsDigitizing), None)

    val (persistedLanes, changeSet) = ServiceWithDao.adjustLanesSideCodes(roadLinkTowards2, Seq(lane11), ChangeSet(), Seq(roadAddress) )

    persistedLanes should have size 2
    persistedLanes.map(_.sideCode) should be (Seq(SideCode.AgainstDigitizing.value, SideCode.TowardsDigitizing.value))
    persistedLanes.map(_.id) should be (Seq(2L, 0L))
    persistedLanes.map(_.linkId) should be (Seq(roadLinkTowards2.linkId, roadLinkTowards2.linkId))

    changeSet.expiredLaneIds should be(Set())

  }

  test("RoadLink TowardsDigitizing and with two main Lanes(TowardsDigitizing and AgainstDigitizing). " +
    "The main lane TowardsDigitizing should be standing.") {
    val roadLinkTowards1 = RoadLink(1L, Seq(Point(0.0, 0.0), Point(10.0, 0.0)), 10.0, Municipality,
                            1, TrafficDirection.TowardsDigitizing, Motorway, None, None)

    val lane11 = PersistedLane(1L, roadLinkTowards1.linkId, SideCode.TowardsDigitizing.value,
                  21, 745L, 0, 10.0, None, None, None, None, expired = false, 0L, None,
                  Seq(LaneProperty("lane_code", Seq(LanePropertyValue(11)))) )

    val lane21 = PersistedLane(2L, roadLinkTowards1.linkId, SideCode.AgainstDigitizing.value,
                  21, 745L, 0, 10.0, None, None, None, None, expired = false, 0L, None,
                  Seq(LaneProperty("lane_code", Seq(LanePropertyValue(21)))) )

    val roadAddress = RoadAddressTEMP(roadLinkTowards1.linkId, 6, 202, Track.Combined, 1, 150, 0, 150,
                        Seq(Point(0.0, 0.0), Point(15.0, 0.0)), Some(SideCode.TowardsDigitizing), None)

    val (persistedLanes, changeSet) = ServiceWithDao.adjustLanesSideCodes(roadLinkTowards1,Seq(lane11, lane21), ChangeSet(), Seq(roadAddress) )


    persistedLanes should have size 1
    persistedLanes.map(_.sideCode) should be (Seq(SideCode.BothDirections.value))
    persistedLanes.map(_.laneCode) should be (List(11))
    persistedLanes.map(_.linkId) should be (Seq(roadLinkTowards1.linkId))

    changeSet.expiredLaneIds should be (Set(1L,2L))
  }

}