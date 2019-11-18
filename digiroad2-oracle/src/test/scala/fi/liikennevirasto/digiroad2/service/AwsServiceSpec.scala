package fi.liikennevirasto.digiroad2.service

import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}
import fi.liikennevirasto.digiroad2.{AssetService, DummyEventBus, Point}
import fi.liikennevirasto.digiroad2.asset.{Freeway, LinkGeomSource, TrafficDirection}
import fi.liikennevirasto.digiroad2.client.vvh.VVHClient
import fi.liikennevirasto.digiroad2.dao.AwsDao
import fi.liikennevirasto.digiroad2.linearasset.{NumericValue, PersistedLinearAsset, RoadLink}
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.service.linearasset._
import fi.liikennevirasto.digiroad2.service.pointasset._
import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.util.TestTransactions
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

class AwsServiceSpec extends FunSuite with Matchers with BeforeAndAfter {
  val mockVVHClient = MockitoSugar.mock[VVHClient]
  val mockOnOffLinearAssetService = MockitoSugar.mock[OnOffLinearAssetService]
  val mockRoadLinkService = MockitoSugar.mock[RoadLinkService]
  val mocklinearAssetService = MockitoSugar.mock[LinearAssetService]
  val mockObstacleService = MockitoSugar.mock[ObstacleService]
  val mockAssetService = MockitoSugar.mock[AssetService]
  val mockSpeedLimitService = MockitoSugar.mock[SpeedLimitService]
  val mockPavedRoadService = MockitoSugar.mock[PavedRoadService]
  val mockRoadWidthService = MockitoSugar.mock[RoadWidthService]
  val mockManoeuvreService = MockitoSugar.mock[ManoeuvreService]
  val mockPedestrianCrossingService = MockitoSugar.mock[PedestrianCrossingService]
  val mockRailwayCrossingService = MockitoSugar.mock[RailwayCrossingService]
  val mockTrafficLightService = MockitoSugar.mock[TrafficLightService]
  val mockMassTransitLaneService = MockitoSugar.mock[MassTransitLaneService]
  val mockNumberOfLanesService = MockitoSugar.mock[NumberOfLanesService]
  val speedLimitService = new SpeedLimitService(new DummyEventBus, mockVVHClient, mockRoadLinkService)


  lazy val dataSource = {
    val cfg = new BoneCPConfig(OracleDatabase.loadProperties("/bonecp.properties"))
    new BoneCPDataSource(cfg)
  }


  val dataSetId = "ab70d6a9-9616-4cc4-abbe-6272c2344709"
  val roadLinksList: List[List[BigInt]] = List(List(441062, 441063, 441070, 452512), List(445212))

  val linaerGeometry = Map(("crs", Map(("type", "name"), ("properties", Map("name" -> "EPSG:3067")))), ("type", "LineString"), ("coordinates", List(List(384594.081, 6674141.478, 105.55299999999988), List(384653.656, 6674029.718, 106.02099999999336), List(384731.654, 6673901.8, 106.37600000000384), List(384919.538, 6673638.735, 106.51600000000326))))
  val pointGeometry = Map(("crs", Map(("type", "name"), ("properties", Map("name" -> "EPSG:3067")))), ("type", "Point"), ("coordinates", List(List(385786, 6671390, 0))))

  val linaerProperties = Map("name" -> "Mannerheimintie", "pavementClass" -> "1", "speedLimit" -> "100", "sideCode" -> 1, "id" -> 9, "functionalClass" -> "Katu", "type" -> "Roadlink")
  val pointProperties = Map("id" -> BigInt(100000), "type" -> "obstacle", "class" -> 1)

  val linearFeatures = Map(("type", "Feature"), ("geometry", linaerGeometry), ("properties", linaerProperties))
  val pointFeatures = Map(("type", "Feature"), ("geometry", pointGeometry), ("properties", pointProperties))

  val communGeoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(linearFeatures, pointFeatures)))


  object ServiceWithDao extends AwsService(mockVVHClient, mockOnOffLinearAssetService, mockRoadLinkService, mocklinearAssetService, mockSpeedLimitService, mockPavedRoadService, mockRoadWidthService, mockManoeuvreService, mockAssetService, mockObstacleService, mockPedestrianCrossingService, mockRailwayCrossingService, mockTrafficLightService, mockMassTransitLaneService, mockNumberOfLanesService){
    override def withDynTransaction[T](f: => T): T = f
    override def withDynSession[T](f: => T): T = f
    override def awsDao: AwsDao = new AwsDao
  }

  def runWithRollback(test: => Unit): Unit = TestTransactions.runWithRollback(dataSource)(test)

  test("number of features doesn't match with the number of list of road links give") {
    val wrongRoadLinksList: List[List[BigInt]] = List(List(441062, 441063, 441070, 452512))
    val dataSet = Dataset(dataSetId, communGeoJson, wrongRoadLinksList)

    runWithRollback {
      ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt
      datasetStatus should be(1)
    }
  }

  test("validate if features have id key/value") {
    val roadLinksList: List[List[BigInt]] = List(List(445212))
    val pointPropertiesWhithoutID = Map("type" -> "obstacle", "class" -> 1)
    val pointFeatures = Map(("type", "Feature"), ("geometry", pointGeometry), ("properties", pointPropertiesWhithoutID))
    val geoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(pointFeatures)))
    val dataSet = Dataset(dataSetId, geoJson, roadLinksList)

    runWithRollback {
      val numberOfFeaturesWithoutId = ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt

      numberOfFeaturesWithoutId should be (1)
      datasetStatus should be(2)
    }
  }

  //TODO: The return of this test need to be revied because doesn't make sence send (0 -> "Inserted successfuly", 1 -> "Errors while validating") at the same time. Need to send error because the Roadlink doesn't exist
  test("validate if roadLink exists on VVH") {
    when(mockRoadLinkService.getRoadsLinksFromVVH(Set(5), false)).thenReturn(Seq())

    val roadLinksList: List[List[BigInt]] = List(List(5))
    val geoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(pointFeatures)))
    val dataSet = Dataset(dataSetId, geoJson, roadLinksList)

    runWithRollback {
      val numberOfFeaturesWithoutId = ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt
      val featuresStatus = ServiceWithDao.awsDao.checkAllFeatureIdAndStatusByDataset(dataSetId)

      numberOfFeaturesWithoutId should be(0)
      datasetStatus should be(2)
      featuresStatus should be (List((100000,"0,1")))
    }
  }

  test("validate if the Geometry Type is one of the allowed") {
    val newRoadLinks = Seq(RoadLink(5000L, List(Point(0.0, 0.0), Point(100.0, 0.0)), 10.0, Municipality, 1, TrafficDirection.BothDirections, Freeway, None, None, Map("MUNICIPALITYCODE" -> BigInt(235))))
    when(mockRoadLinkService.getRoadsLinksFromVVH(Set(5000), false)).thenReturn(newRoadLinks)

    val roadLinksList: List[List[BigInt]] = List(List(5000))
    val pointGeometryWrong = Map(("crs", Map(("type", "name"), ("properties", Map("name" -> "EPSG:3067")))), ("type", "Linear"), ("coordinates", List(List(50, 0, 0))))
    val pointFeaturesWrong = Map(("type", "Feature"), ("geometry", pointGeometryWrong), ("properties", pointProperties))
    val geoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(pointFeaturesWrong)))
    val dataSet = Dataset(dataSetId, geoJson, roadLinksList)

    runWithRollback {
      val numberOfFeaturesWithoutId = ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt
      val featuresStatus = ServiceWithDao.awsDao.checkAllFeatureIdAndStatusByDataset(dataSetId)

      numberOfFeaturesWithoutId should be(0)
      datasetStatus should be(2)
      featuresStatus should be (List((100000,"0,1")))
    }
  }


  test("new obstacle with nonvalid value to be created/updated") {
    val newRoadLinks = Seq(RoadLink(5000L, List(Point(0.0, 0.0), Point(100.0, 0.0)), 10.0, Municipality, 1, TrafficDirection.BothDirections, Freeway, None, None, Map("MUNICIPALITYCODE" -> BigInt(235))))
    when(mockRoadLinkService.getRoadsLinksFromVVH(Set(5000), false)).thenReturn(newRoadLinks)

    val pointPropertiesWrong = Map("id" -> BigInt(100000), "type" -> "obstacle", "class" -> 9)
    val roadLinksList: List[List[BigInt]] = List(List(5000))
    val pointGeometryWrong = Map(("crs", Map(("type", "name"), ("properties", Map("name" -> "EPSG:3067")))), ("type", "Point"), ("coordinates", List(List(50, 0, 0))))
    val pointFeaturesWrong = Map(("type", "Feature"), ("geometry", pointGeometryWrong), ("properties", pointPropertiesWrong))
    val geoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(pointFeaturesWrong)))
    val dataSet = Dataset(dataSetId, geoJson, roadLinksList)

    runWithRollback {
      val numberOfFeaturesWithoutId = ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt
      val featuresStatus = ServiceWithDao.awsDao.checkAllFeatureIdAndStatusByDataset(dataSetId)

      numberOfFeaturesWithoutId should be(0)
      datasetStatus should be(2)
      featuresStatus should be (List((100000,"0,5")))
    }
  }

  test("new speedlimit with nonvalid value to be created/updated") {
    val newRoadLinks = Seq(RoadLink(5000L, List(Point(0.0, 0.0), Point(100.0, 0.0)), 10.0, Municipality, 1, TrafficDirection.BothDirections, Freeway, None, None, Map("MUNICIPALITYCODE" -> BigInt(235))))
    when(mockRoadLinkService.getRoadsLinksFromVVH(Set(5000), false)).thenReturn(newRoadLinks)

    val roadLinksList: List[List[BigInt]] = List(List(5000))
    val linaerPropertiesWrong = Map("name" -> "Mannerheimintie", "speedLimit" -> "110", "sideCode" -> BigInt(1), "id" -> BigInt(200000), "functionalClass" -> "Katu", "type" -> "Roadlink")
    val linaerGeometry = Map(("crs", Map(("type", "name"), ("properties", Map("name" -> "EPSG:3067")))), ("type", "LineString"), ("coordinates", List(List(0.0, 0.0, 0), List(100.0, 0.0, 0))))
    val linearFeaturesWrong = Map(("type", "Feature"), ("geometry", linaerGeometry), ("properties", linaerPropertiesWrong))
    val geoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(linearFeaturesWrong)))
    val dataSet = Dataset(dataSetId, geoJson, roadLinksList)

    runWithRollback {
      val numberOfFeaturesWithoutId = ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt
      val featuresStatus = ServiceWithDao.awsDao.checkAllFeatureIdAndStatusByDataset(dataSetId)

      numberOfFeaturesWithoutId should be(0)
      datasetStatus should be(2)
      featuresStatus should be (List((200000,"0,3")))
    }
  }

  test("new pavementClass with nonvalid value to be created/updated") {
    val newRoadLinks = Seq(RoadLink(5000L, List(Point(0.0, 0.0), Point(100.0, 0.0)), 10.0, Municipality, 1, TrafficDirection.BothDirections, Freeway, None, None, Map("MUNICIPALITYCODE" -> BigInt(235))))
    when(mockRoadLinkService.getRoadsLinksFromVVH(Set(5000), false)).thenReturn(newRoadLinks)

    val roadLinksList: List[List[BigInt]] = List(List(5000))
    val linaerPropertiesWrong = Map("name" -> "Mannerheimintie", "pavementClass" -> "60", "sideCode" -> BigInt(1), "id" -> BigInt(200000), "functionalClass" -> "Katu", "type" -> "Roadlink")
    val linaerGeometry = Map(("crs", Map(("type", "name"), ("properties", Map("name" -> "EPSG:3067")))), ("type", "LineString"), ("coordinates", List(List(0.0, 0.0, 0), List(100.0, 0.0, 0))))
    val linearFeaturesWrong = Map(("type", "Feature"), ("geometry", linaerGeometry), ("properties", linaerPropertiesWrong))
    val geoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(linearFeaturesWrong)))
    val dataSet = Dataset(dataSetId, geoJson, roadLinksList)

    runWithRollback {
      val numberOfFeaturesWithoutId = ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt
      val featuresStatus = ServiceWithDao.awsDao.checkAllFeatureIdAndStatusByDataset(dataSetId)

      numberOfFeaturesWithoutId should be(0)
      datasetStatus should be(2)
      featuresStatus should be (List((200000,"0,4")))
    }
  }

  test("new speedlimit with valid value to be created") {
//    val testSpeedLimitProvider = new SpeedLimitService(new DummyEventBus, mockVVHClient, mockRoadLinkService)

    val newRoadLink = RoadLink(5000L, List(Point(0.0, 0.0), Point(100.0, 0.0)), 10.0, Municipality, 1, TrafficDirection.BothDirections, Freeway, None, None, Map("MUNICIPALITYCODE" -> BigInt(235)))
    when(mockRoadLinkService.getRoadsLinksFromVVH(Set(5000), false)).thenReturn(Seq(newRoadLink))

    val roadLinksList: List[List[BigInt]] = List(List(5000))
    val linaerPropertiesWrong = Map("name" -> "Mannerheimintie", "speedLimit" -> "100", "sideCode" -> BigInt(1), "id" -> BigInt(200000), "functionalClass" -> "Katu", "type" -> "Roadlink")
    val linaerGeometry = Map(("crs", Map(("type", "name"), ("properties", Map("name" -> "EPSG:3067")))), ("type", "LineString"), ("coordinates", List(List(0.0, 0.0, 0), List(100.0, 0.0, 0))))
    val linearFeaturesWrong = Map(("type", "Feature"), ("geometry", linaerGeometry), ("properties", linaerPropertiesWrong))
    val geoJson: Map[String, Any] = Map(("type", "FeatureCollection"), ("features", List(linearFeaturesWrong)))
    val dataSet = Dataset(dataSetId, geoJson, roadLinksList)

    runWithRollback {
      val numberOfFeaturesWithoutId = ServiceWithDao.validateAndInsertDataset(dataSet)
      val datasetStatus = ServiceWithDao.awsDao.checkDatasetStatus(dataSetId).toInt
      val featuresStatus = ServiceWithDao.awsDao.checkAllFeatureIdAndStatusByDataset(dataSetId)

      numberOfFeaturesWithoutId should be(0)
      datasetStatus should be(0)
      featuresStatus should be (List((200000,"0")))

      val createdSpeedLimit = mockSpeedLimitService.getExistingAssetByRoadLink(newRoadLink, false)

      createdSpeedLimit.size should be(1)
      createdSpeedLimit.head.value should be(Some(100))
    }
  }

}
