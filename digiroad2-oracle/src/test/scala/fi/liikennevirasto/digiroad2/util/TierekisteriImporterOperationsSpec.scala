package fi.liikennevirasto.digiroad2.util

import fi.liikennevirasto.digiroad2.Point
import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.client.tierekisteri._
import fi.liikennevirasto.digiroad2.client.tierekisteri.importer._
import fi.liikennevirasto.digiroad2.client.vvh.{FeatureClass, VVHClient, VVHRoadLinkClient, VVHRoadlink}
import fi.liikennevirasto.digiroad2.dao.{MunicipalityDao, OracleAssetDao, RoadAddressDAO, RoadAddress => ViiteRoadAddress}
import fi.liikennevirasto.digiroad2.dao.linearasset.{OracleLinearAssetDao, OracleSpeedLimitDao}
import fi.liikennevirasto.digiroad2.linearasset.{NumericValue, TextualValue}
import fi.liikennevirasto.digiroad2.service.{RoadAddressesService, RoadLinkService}
import fi.liikennevirasto.digiroad2.service.linearasset.LinearAssetTypes
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class TierekisteriImporterOperationsSpec extends FunSuite with Matchers  {

  val mockAssetDao: OracleAssetDao = MockitoSugar.mock[OracleAssetDao]
  val mockRoadAddressDAO: RoadAddressDAO = MockitoSugar.mock[RoadAddressDAO]
  val mockRoadAddressService = MockitoSugar.mock[RoadAddressesService]
  val mockMunicipalityDao: MunicipalityDao = MockitoSugar.mock[MunicipalityDao]
  val mockTRClient: TierekisteriLightingAssetClient = MockitoSugar.mock[TierekisteriLightingAssetClient]
  val mockTRTrafficSignsLimitClient: TierekisteriTrafficSignAssetClient = MockitoSugar.mock[TierekisteriTrafficSignAssetClient]
  val mockTRTrafficSignsLimitSpeedLimitClient: TierekisteriTrafficSignSpeedLimitClient = MockitoSugar.mock[TierekisteriTrafficSignSpeedLimitClient]
  val mockRoadLinkService: RoadLinkService = MockitoSugar.mock[RoadLinkService]
  val mockVVHClient: VVHClient = MockitoSugar.mock[VVHClient]
  val mockVVHRoadLinkClient: VVHRoadLinkClient = MockitoSugar.mock[VVHRoadLinkClient]
  val linearAssetDao = new OracleLinearAssetDao(mockVVHClient, mockRoadLinkService)
  val speedLimitDao = new OracleSpeedLimitDao(mockVVHClient, mockRoadLinkService)
  val oracleAssetDao = new OracleAssetDao
  val mockTRPavedRoadClient: TierekisteriPavedRoadAssetClient = MockitoSugar.mock[TierekisteriPavedRoadAssetClient]
  val mockMassTransitLaneClient: TierekisteriMassTransitLaneAssetClient = MockitoSugar.mock[TierekisteriMassTransitLaneAssetClient]
  val mockTRDamageByThawClient: TierekisteriDamagedByThawAssetClient = MockitoSugar.mock[TierekisteriDamagedByThawAssetClient]
  val mockTREuropeanRoadClient: TierekisteriEuropeanRoadAssetClient = MockitoSugar.mock[TierekisteriEuropeanRoadAssetClient]
  val mockTRSpeedLimitAssetClient: TierekisteriSpeedLimitAssetClient = MockitoSugar.mock[TierekisteriSpeedLimitAssetClient]

  lazy val roadWidthImporterOperations: RoadWidthTierekisteriImporter = {
    new RoadWidthTierekisteriImporter()
  }

  lazy val litRoadImporterOperations: LitRoadTierekisteriImporter = {
    new LitRoadTierekisteriImporter()
  }

  lazy val tierekisteriAssetImporterOperations: TestTierekisteriAssetImporterOperations = {
    new TestTierekisteriAssetImporterOperations
  }

  lazy val speedLimitsTierekisteriImporter: TestSpeedLimitsTierekisteriImporter = {
    new TestSpeedLimitsTierekisteriImporter
  }

  lazy val speedLimitAssetTierekisteriImporter: TestSpeedLimitAssetOperations ={
    new TestSpeedLimitAssetOperations
  }

  class TestTierekisteriAssetImporterOperations extends TierekisteriAssetImporterOperations {
    override def typeId: Int = 999

    override def withDynSession[T](f: => T): T = f

    override def withDynTransaction[T](f: => T): T = f

    override def assetName: String = "assetTest"

    override type TierekisteriClientType = TierekisteriAssetDataClient
    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient: TierekisteriLightingAssetClient = mockTRClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient

    //Creating this new methods because is protected visibility on the trait
    def getRoadAddressSectionsTest(trAsset: TierekisteriAssetData): Seq[(AddressSection, TierekisteriAssetData)] = super.getRoadAddressSections(trAsset)

//    def getAllViiteRoadAddressTest(section: AddressSection) = super.getAllViiteRoadAddress(section)
//
//    def getAllViiteRoadAddressTest(roadNumber: Long, roadPart: Long) = super.getAllViiteRoadAddress(roadNumber, roadPart)
//
//    def getAllViiteRoadAddressTest(roadNumber: Long, tracks: Seq[Track]) = super.getAllViiteRoadAddress(roadNumber: Long, tracks: Seq[Track])

    def expireAssetsTest(linkIds: Seq[Long]): Unit = super.expireAssets(linkIds)

    def calculateStartLrmByAddressTest(startAddress: ViiteRoadAddress, section: AddressSection): Option[Double] = super.calculateStartLrmByAddress(startAddress, section)

    def calculateEndLrmByAddressTest(endAddress: ViiteRoadAddress, section: AddressSection): Option[Double] = super.calculateEndLrmByAddress(endAddress, section)

    def getAllTierekisteriAddressSectionsTest(roadNumber: Long) = super.getAllTierekisteriAddressSections(roadNumber: Long)

    def getAllTierekisteriAddressSectionsTest(roadNumber: Long, roadPart: Long) = super.getAllTierekisteriAddressSections(roadNumber: Long)

    def getAllTierekisteriHistoryAddressSectionTest(roadNumber: Long, lastExecution: DateTime) = super.getAllTierekisteriHistoryAddressSection(roadNumber: Long, lastExecution: DateTime)

    override protected def createAsset(section: AddressSection, trAssetData: TierekisteriAssetData, existingRoadAddresses: Seq[ViiteRoadAddress]): Unit = {

    }
  }

  class TestSpeedLimitsTierekisteriImporter extends StateSpeedLimitTierekisteriImporter {
    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient = mockTRTrafficSignsLimitSpeedLimitClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient
    override def withDynTransaction[T](f: => T): T = f

    def testSplitRoadAddressSectionBySigns(trAssets: Seq[TierekisteriAssetData], ra: ViiteRoadAddress, roadSide: RoadSide): Seq[(AddressSection, Option[TierekisteriAssetData])] = {
      super.splitRoadAddressSectionBySigns(trAssets, ra, roadSide)
    }

    def calculateMeasuresTest(roadAddress: ViiteRoadAddress, section: AddressSection) = super.calculateMeasures(roadAddress, section)

    def createSpeedLimitTest(roadAddress: ViiteRoadAddress, addressSection: AddressSection,
                             trAssetOption: Option[TrAssetInfo], roadLinkOption: Option[VVHRoadlink]) = super.createSpeedLimit(roadAddress, addressSection, trAssetOption, roadLinkOption, None)

    override def createUrbanTrafficSign(roadLink: Option[VVHRoadlink], trUrbanAreaAssets: Seq[TierekisteriUrbanAreaData],
                                        addressSection: AddressSection, roadAddress: ViiteRoadAddress,
                                        roadSide: RoadSide): Option[TrAssetInfo] = super.createUrbanTrafficSign(roadLink, trUrbanAreaAssets, addressSection, roadAddress, roadSide)

    def generateOneSideSpeedLimitsTest(roadNumber: Long, roadSide: RoadSide, trAssets : Seq[TierekisteriAssetData], trUrbanAreaAssets: Seq[TierekisteriUrbanAreaData], existingRoadAddresses: Seq[ViiteRoadAddress])
    = super.generateOneSideSpeedLimits(roadNumber: Long, roadSide: RoadSide, trAssets : Seq[TierekisteriAssetData], trUrbanAreaAssets: Seq[TierekisteriUrbanAreaData], existingRoadAddresses: Seq[ViiteRoadAddress])
  }

  class TestLitRoadOperations extends LitRoadTierekisteriImporter {
    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient: TierekisteriLightingAssetClient = mockTRClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient
    override def withDynTransaction[T](f: => T): T = f
  }

  class TestPavedRoadOperations extends PavedRoadTierekisteriImporter {
    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient: TierekisteriPavedRoadAssetClient = mockTRPavedRoadClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient
    override def withDynTransaction[T](f: => T): T = f

    def filterTierekisteriAssetsTest(tierekisteriAssetData: TierekisteriAssetData) = super.filterTierekisteriAssets(tierekisteriAssetData)
  }

  class TestTierekisteriAssetImporterOperationsFilterAll extends TestTierekisteriAssetImporterOperations {
      protected override def filterTierekisteriAssets(tierekisteriAssetData: TierekisteriAssetData) : Boolean = {
        false
      }
  }

  class TestMassTransitLaneOperations extends MassTransitLaneTierekisteriImporter {

    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient: TierekisteriMassTransitLaneAssetClient = mockMassTransitLaneClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient
    override def withDynTransaction[T](f: => T): T = f

    def filterTierekisteriAssetsTest(tierekisteriAssetData: TierekisteriAssetData) = super.filterTierekisteriAssets(tierekisteriAssetData)
  }

  class TestDamageByThawOperations extends DamagedByThawTierekisteriImporter {
    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient: TierekisteriDamagedByThawAssetClient = mockTRDamageByThawClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient
    override def withDynTransaction[T](f: => T): T = f
  }

  class TestEuropeanRoadOperations extends EuropeanRoadTierekisteriImporter {
    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient: TierekisteriEuropeanRoadAssetClient = mockTREuropeanRoadClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient
    override def withDynTransaction[T](f: => T): T = f
  }

  class TestSpeedLimitAssetOperations extends SpeedLimitTierekisteriImporter {
    override lazy val assetDao: OracleAssetDao = mockAssetDao
    override lazy val municipalityDao: MunicipalityDao = mockMunicipalityDao
    override lazy val roadAddressService: RoadAddressesService = mockRoadAddressService
    override val tierekisteriClient: TierekisteriSpeedLimitAssetClient = mockTRSpeedLimitAssetClient
    override lazy val roadLinkService: RoadLinkService = mockRoadLinkService
    override lazy val vvhClient: VVHClient = mockVVHClient
    override def withDynTransaction[T](f: => T): T = f
  }

  test("assets splited are split properly") {
    val trl = TierekisteriLightingData(4L, 203L, 208L, Track.RightSide, 3184L, 6584L)
    val sections = tierekisteriAssetImporterOperations.getRoadAddressSectionsTest(trl).map(_._1)
    sections.size should be (6)
    sections.head should be (AddressSection(4L, 203L, Track.RightSide, 3184L, None))
    sections.last should be (AddressSection(4L, 208L, Track.RightSide,  0L, Some(6584L)))
    val middleParts = sections.filterNot(s => s.roadPartNumber==203L || s.roadPartNumber==208L)
    middleParts.forall(s => s.track == Track.RightSide) should be (true)
    middleParts.forall(s => s.startAddressMValue == 0L) should be (true)
    middleParts.forall(s => s.endAddressMValue.isEmpty) should be (true)
  }

  test("assets split works on single part") {
    val trl = TierekisteriLightingData(4L, 203L, 203L, Track.RightSide, 3184L, 6584L)
    val sections = tierekisteriAssetImporterOperations.getRoadAddressSectionsTest(trl).map(_._1)
    sections.size should be (1)
    sections.head should be (AddressSection(4L, 203L, Track.RightSide, 3184L, Some(6584L)))
  }

  test("assets split works on two parts") {
    val trl = TierekisteriLightingData(4L, 203L, 204L, Track.RightSide, 3184L, 6584L)
    val sections = tierekisteriAssetImporterOperations.getRoadAddressSectionsTest(trl).map(_._1)
    sections.size should be (2)
    sections.head should be (AddressSection(4L, 203L, Track.RightSide, 3184L, None))
    sections.last should be (AddressSection(4L, 204L, Track.RightSide, 0L, Some(6584L)))
  }

  test("assets get tierekisteri data with address section") {
    val assetValue = 10
    val trl = TierekisteriLightingData(4L, 203L, 203L, Track.RightSide, 3184L, 6584L)
    val sectionTrAssetPair = tierekisteriAssetImporterOperations.getRoadAddressSectionsTest(trl)
    sectionTrAssetPair.size should be (1)
    sectionTrAssetPair.head should be ((AddressSection(4L, 203L, Track.RightSide, 3184L, Some(6584L)), trl))
  }

  test("calculate lrm position from road address when section inside road address and is towards digitizing") {
    val startAddressMValue = 0
    val endAddressMValue = 100
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 50, Some(100))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultStartMValue should be (Some(6.0))
    resultEndMValue should be (Some(11.0))
  }

  test("calculate lrm position from road address when section inside road address and is against digitizing") {
    val startAddressMValue = 0
    val endAddressMValue = 100
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 50, Some(100))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultEndMValue should be (Some(1.0))
    resultStartMValue should be (Some(6.0))
  }

  test("calculate lrm position from road address when section starts outside the road address and is towards digitizing") {
    val startAddressMValue = 100
    val endAddressMValue = 200
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 50, Some(150))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultStartMValue should be (Some(1.0))
    resultEndMValue should be (Some(6.0))
  }

  test("calculate lrm position from road address when section ends outside the road address and is towards digitizing") {
    val startAddressMValue = 100
    val endAddressMValue = 200
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 150, Some(250))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultStartMValue should be (Some(6.0))
    resultEndMValue should be (Some(11.0))
  }

  test("calculate lrm position from road address when section starts outside the road address and is against digitizing") {
    val startAddressMValue = 100
    val endAddressMValue = 200
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 50, Some(150))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultStartMValue should be (Some(11.0))
    resultEndMValue should be (Some(6.0))
  }

  test("calculate lrm position from road address when section ends outside the road address and is against digitizing") {
    val startAddressMValue = 100
    val endAddressMValue = 200
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 150, Some(250))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultStartMValue should be (Some(6.0))
    resultEndMValue should be (Some(1.0))
  }

  test("calculate lrm position from road address when section starts and ends outside the road address and is towards digitizing") {
    val startAddressMValue = 100
    val endAddressMValue = 200
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 50, Some(250))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultStartMValue should be (Some(1.0))
    resultEndMValue should be (Some(11.0))
  }

  test("calculate lrm position from road address when section starts and ends outside the road address and is against digitizing") {
    val startAddressMValue = 100
    val endAddressMValue = 200
    val startMValue = 0
    val endMValue = 10
    val roadAddress = ViiteRoadAddress(1L, 100, 1, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1, 11, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
    val addressSection = AddressSection(1, 1, Track.Combined, 50, Some(250))
    val resultStartMValue = tierekisteriAssetImporterOperations.calculateStartLrmByAddressTest(roadAddress, addressSection)
    val resultEndMValue = tierekisteriAssetImporterOperations.calculateEndLrmByAddressTest(roadAddress, addressSection)

    resultStartMValue should be (Some(11.0))
    resultEndMValue should be (Some(1.0))
  }

  test("calculate measures, towards digitizing"){
    TestTransactions.runWithRollback() {

      val testLitRoad = new TestLitRoadOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 500L

      val starAddress = 0
      val endAddress = 500

      val startSection = 50
      val endSection = 350

      val tr = TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection)
      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testLitRoad.importAssets()
      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(testLitRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).head

      asset.linkId should be (5001)
      asset.value should be (Some(NumericValue(1)))
      asset.startMeasure should be (50)
      asset.endMeasure should be (350)
      asset.sideCode should be (1)
    }
  }

  test("calculate measures, with 2 sections"){
    TestTransactions.runWithRollback() {

      val testLitRoad = new TestLitRoadOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 201L
      val startMValueSection1 = 0L
      val endMValueSection1 = 500L

      val startMValueSection2 = 0L
      val endMValueSection2 = 1000L

      val starAddressSection1 = 50
      val endAddressSection1 = 500

      val starAddressSection2 = 500
      val endAddressSection2= 750

      val startSection = 0
      val endSection = 1000

      val raS1 = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startMValueSection1, endMValueSection1, None, None, 5001, starAddressSection1, endAddressSection1, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val raS2 = ViiteRoadAddress(1L, roadNumber, endRoadPartNumber, Track.RightSide, startMValueSection2, endMValueSection2, None, None, 5001, starAddressSection2, endAddressSection2, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)

      val tr = TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection)
      val vvhRoadLink = VVHRoadlink(5001, 235, List(Point(0.0, 0.0), Point(0.0, 1000.0)), State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(raS1)).thenReturn(Seq(raS2))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testLitRoad.importAssets()
      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(testLitRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId)

      asset.length should be (2)
      asset.count(a => a.startMeasure == 50) should be (1)
      asset.count(a => a.endMeasure == 500) should be (1)
      asset.count(a => a.startMeasure == 500) should be (1)
      asset.count(a => a.endMeasure == 750) should be (1)
    }
  }

  test("import assets (litRoad) from TR to OTH") {
     TestTransactions.runWithRollback() {

       val testLitRoad = new TestLitRoadOperations
       val roadNumber = 4L
       val startRoadPartNumber = 200L
       val endRoadPartNumber = 200L
       val startAddressMValue = 0L
       val endAddressMValue = 250L

       val tr = TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue)
       val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1.5, 11.4, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
       val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

       testLitRoad.importAssets()
       val asset = linearAssetDao.fetchLinearAssetsByLinkIds(testLitRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).head

       asset.linkId should be(5001)
       asset.value should be(Some(NumericValue(1)))
     }
   }

  test("update assets (litRoad) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testLitRoad = new TestLitRoadOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L

      val starAddress = 0
      val endAddress = 500

      val startSection = 50
      val endSection = 150

      val startSectionHist = 100
      val endSectionHist = 150

      val tr = TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection)
      val trHist = TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSectionHist, endSectionHist)

      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockTRClient.fetchHistoryAssetData(any[Long], any[Option[DateTime]])).thenReturn(Seq(trHist))
      when(mockTRClient.fetchActiveAssetData(any[Long], any[Long])).thenReturn(Seq(trHist))

      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testLitRoad.importAssets()
      val assetI = linearAssetDao.fetchLinearAssetsByLinkIds(testLitRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).head

      testLitRoad.updateAssets(DateTime.now())
      val assetU = linearAssetDao.fetchLinearAssetsByLinkIds(testLitRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).filterNot(a => a.id == assetI.id).head

      assetU.startMeasure should not be assetI.startMeasure
      assetU.endMeasure should be (assetI.endMeasure)
    }
  }

  test("import assets (massTransitLane) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testMassTransitLane = new TestMassTransitLaneOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val trLaneType = TRLaneArrangementType.apply(5)

      val tr = TierekisteriMassTransitLaneData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, trLaneType)
      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1.5, 11.4, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockMassTransitLaneClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testMassTransitLane.importAssets()
      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(testMassTransitLane.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).head

      asset.linkId should be (5001)
      asset.value should be (Some(NumericValue(1)))
    }
  }

  test("update assets (massTransitLane) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testMassTransitLane = new TestMassTransitLaneOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val laneType = TRLaneArrangementType.apply(5)

      val starAddress = 0
      val endAddress = 500

      val startSection = 50
      val endSection = 150

      val starSectionHist = 100
      val endSectionHist = 150

      val tr = TierekisteriMassTransitLaneData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection, laneType)
      val trHist = TierekisteriMassTransitLaneData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, starSectionHist, endSectionHist, laneType)

      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockMassTransitLaneClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockMassTransitLaneClient.fetchHistoryAssetData(any[Long], any[Option[DateTime]])).thenReturn(Seq(trHist))
      when(mockMassTransitLaneClient.fetchActiveAssetData(any[Long], any[Long])).thenReturn(Seq(trHist))

      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testMassTransitLane.importAssets()
      val assetI = linearAssetDao.fetchLinearAssetsByLinkIds(testMassTransitLane.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).head

      testMassTransitLane.updateAssets(DateTime.now())
      val assetU = linearAssetDao.fetchLinearAssetsByLinkIds(testMassTransitLane.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).filterNot(a => a.id == assetI.id).head

      assetU.startMeasure should not be assetI.startMeasure
      assetU.endMeasure should be (assetI.endMeasure)
    }
  }

  test("import assets (pavedRoad) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testPavedRoad = new TestPavedRoadOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L

      val tr = TierekisteriPavedRoadData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, TRPavedRoadType.Cobblestone)
      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1.5, 11.4, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRPavedRoadClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testPavedRoad.importAssets()
      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(testPavedRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).head

      asset.linkId should be (5001)
      asset.value should be (Some(NumericValue(1)))
    }
  }

  test("update assets (pavedRoad) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testPavedRoad = new TestPavedRoadOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L

      val starAddress = 0
      val endAddress = 500

      val startSection = 50
      val endSection = 150

      val starSectionHist = 100
      val endSectionHist = 150

      val tr = TierekisteriPavedRoadData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection, TRPavedRoadType.Cobblestone)
      val trHist = TierekisteriPavedRoadData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, starSectionHist, endSectionHist, TRPavedRoadType.Cobblestone)

      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRPavedRoadClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockTRPavedRoadClient.fetchHistoryAssetData(any[Long], any[Option[DateTime]])).thenReturn(Seq(trHist))
      when(mockTRPavedRoadClient.fetchActiveAssetData(any[Long], any[Long])).thenReturn(Seq(trHist))

      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testPavedRoad.importAssets()
      val assetI = linearAssetDao.fetchLinearAssetsByLinkIds(testPavedRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).head

      testPavedRoad.updateAssets(DateTime.now())
      val assetU = linearAssetDao.fetchLinearAssetsByLinkIds(testPavedRoad.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId).filterNot(a => a.id == assetI.id).head

      assetU.startMeasure should not be assetI.startMeasure
      assetU.endMeasure should be (assetI.endMeasure)
    }
  }

  test("Should allow all asset\"") {
    TestTransactions.runWithRollback() {

      val testTierekisteriAsset = new TestTierekisteriAssetImporterOperations
      val roadNumber = 4L
      val roadPartNumber = 1L
      val startSection = 50
      val endSection = 350
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val date = DateTime.now()

      val tr = Seq(TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection),
        TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection),
        TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.LeftSide, startSection, endSection))

      when(mockTRClient.fetchActiveAssetData(roadNumber)).thenReturn(tr)
      val filterAsset = testTierekisteriAsset.getAllTierekisteriAddressSectionsTest(roadNumber)
      filterAsset.length should be(3)

      when(mockTRClient.fetchActiveAssetData(roadNumber, roadPartNumber)).thenReturn(tr)
      val assetFetch = testTierekisteriAsset.getAllTierekisteriAddressSectionsTest(roadNumber, roadPartNumber)
      assetFetch.length should be(3)

      when(mockTRClient.fetchHistoryAssetData(roadNumber, Some(date))).thenReturn(tr)
      val assetHist = testTierekisteriAsset.getAllTierekisteriHistoryAddressSectionTest(roadNumber, date)
      assetHist.length should be(3)
    }
  }

  test("Should exclude all assets\"") {
    TestTransactions.runWithRollback() {

      val testTierekisteriAsset = new TestTierekisteriAssetImporterOperationsFilterAll
      val roadNumber = 4L
      val roadPartNumber = 1L
      val startSection = 50
      val endSection = 350
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val date = DateTime.now()

      val tr = Seq(TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection),
        TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection),
        TierekisteriLightingData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.LeftSide, startSection, endSection))

      when(mockTRClient.fetchActiveAssetData(roadNumber)).thenReturn(tr)
      val filterAsset = testTierekisteriAsset.getAllTierekisteriAddressSectionsTest(roadNumber)
      filterAsset.length should be(0)

      when(mockTRClient.fetchActiveAssetData(roadNumber, roadPartNumber)).thenReturn(tr)
      val assetFetch = testTierekisteriAsset.getAllTierekisteriAddressSectionsTest(roadNumber, roadPartNumber)
      assetFetch.length should be(0)

      when(mockTRClient.fetchHistoryAssetData(roadNumber, Some(date))).thenReturn(tr)
      val assetHist = testTierekisteriAsset.getAllTierekisteriHistoryAddressSectionTest(roadNumber, date)
      assetHist.length should be(0)
    }
  }

  test("Should not create/update asset (massTransitLane) with TR type Unknown\"") {
    TestTransactions.runWithRollback() {

      val testMassTransitLane = new TestMassTransitLaneOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startSection = 50
      val endSection = 350

      val tr = TierekisteriMassTransitLaneData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection, TRLaneArrangementType.apply(1))
      testMassTransitLane.filterTierekisteriAssetsTest(tr) should be (false)
    }
  }

  test("Should not create/update asset (paved road) with TR type Unknown\"") {
    TestTransactions.runWithRollback() {

      val testPavedRoad = new TestPavedRoadOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startSection = 50
      val endSection = 350

      val tr = TierekisteriPavedRoadData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startSection, endSection, TRPavedRoadType.apply(30))
      testPavedRoad.filterTierekisteriAssetsTest(tr) should be (false)
    }
  }

  test("Should create asset with TR type valid") {
    TestTransactions.runWithRollback() {

      val testDamageByThaw = new TestDamageByThawOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L

      val tr = TierekisteriDamagedByThawData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue)
      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, 100, endAddressMValue, None, None, 5001, 0, 300, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)

      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRDamageByThawClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testDamageByThaw.importAssets()
      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(testDamageByThaw.typeId, Seq(5001), LinearAssetTypes.numericValuePropertyId)
      asset.length should be(1)
    }
  }

  test("import assets (europeanRoad) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testEuropeanRoad = new TestEuropeanRoadOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val assetValue = "E35"

      val tr = TierekisteriEuropeanRoadData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, assetValue)
      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1.5, 11.4, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTREuropeanRoadClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testEuropeanRoad.importAssets()
      val asset = linearAssetDao.fetchAssetsWithTextualValuesByLinkIds(testEuropeanRoad.typeId, Seq(5001), LinearAssetTypes.europeanRoadPropertyId).head

      asset.linkId should be (5001)
      asset.value should be (Some(TextualValue(assetValue)))
    }
  }

  test("update assets (europeanRoad) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testEuropeanRoad = new TestEuropeanRoadOperations

      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val assetValue = "E35"

      val endAddressMValueHist = 200L
      val assetValueHist = "E35, E38"

      val tr = TierekisteriEuropeanRoadData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, assetValue)
      val trHist = TierekisteriEuropeanRoadData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValueHist, assetValueHist)

      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1.5, 11.4, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTREuropeanRoadClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockTREuropeanRoadClient.fetchHistoryAssetData(any[Long], any[Option[DateTime]])).thenReturn(Seq(trHist))
      when(mockTREuropeanRoadClient.fetchActiveAssetData(any[Long], any[Long])).thenReturn(Seq(trHist))

      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))

      testEuropeanRoad.importAssets()
      val assetI = linearAssetDao.fetchAssetsWithTextualValuesByLinkIds(testEuropeanRoad.typeId, Seq(5001), LinearAssetTypes.europeanRoadPropertyId).head

      testEuropeanRoad.updateAssets(DateTime.now())
      val assetU = linearAssetDao.fetchAssetsWithTextualValuesByLinkIds(testEuropeanRoad.typeId, Seq(5001), LinearAssetTypes.europeanRoadPropertyId).filterNot(a => a.id == assetI.id).head

      assetU.startMeasure should be (assetI.startMeasure)
      assetU.endMeasure should not be assetI.endMeasure
      assetU.value should be (Some(TextualValue(assetValueHist)))
    }
  }

  test("Split road address in right side with one sign"){
    val roadNumber = 1
    val roadPart = 1
    val startAddressMValue = 0
    val endAddressMValue = 500
    val starAddress = 0
    val endAddress = 500
    val trAssets = Seq(TierekisteriTrafficSignData(roadNumber, roadPart, roadPart, Track.RightSide, 40, 40, RoadSide.Right, TRTrafficSignType.SpeedLimit, "80"))
    val roadAddress = ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)

    val sections = speedLimitsTierekisteriImporter.testSplitRoadAddressSectionBySigns(trAssets, roadAddress, RoadSide.Right)

    sections.size should be (2)
    val (firstSection, firstTrAsset) = sections.head
    val (lastSection, lastTrAsset) = sections.last

    firstSection.roadNumber should be (1)
    firstSection.roadPartNumber should be (1)
    firstSection.startAddressMValue should be (0)
    firstSection.endAddressMValue should be (Some(40))
    firstTrAsset should be (None)

    lastSection.roadNumber should be (1)
    lastSection.roadPartNumber should be (1)
    lastSection.startAddressMValue should be (40)
    lastSection.endAddressMValue should be (Some(500))
    lastTrAsset should be (trAssets.headOption)
  }

  test("Split road address in right side with two sign"){
    val roadNumber = 1
    val roadPart = 1
    val startAddressMValue = 0
    val endAddressMValue = 500
    val starAddress = 0
    val endAddress = 500
    val trAssets = Seq(
      TierekisteriTrafficSignData(roadNumber, roadPart, roadPart, Track.RightSide, 40, 40, RoadSide.Right, TRTrafficSignType.SpeedLimit, "80"),
      TierekisteriTrafficSignData(roadNumber, roadPart, roadPart, Track.RightSide, 70, 70, RoadSide.Right, TRTrafficSignType.SpeedLimit, "90")
    )
    val roadAddress = ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)

    val sections = speedLimitsTierekisteriImporter.testSplitRoadAddressSectionBySigns(trAssets, roadAddress, RoadSide.Right)

    sections.size should be (3)
    val (firstSection, firstTrAsset) = sections.head
    val (middleSection, middleTrAsset) = sections.tail.head
    val (lastSection, lastTrAsset) = sections.tail.last

    firstSection.roadNumber should be (1)
    firstSection.roadPartNumber should be (1)
    firstSection.startAddressMValue should be (0)
    firstSection.endAddressMValue should be (Some(40))
    firstTrAsset should be (None)

    middleSection.roadNumber should be (1)
    middleSection.roadPartNumber should be (1)
    middleSection.startAddressMValue should be (40)
    middleSection.endAddressMValue should be (Some(70))
    middleTrAsset should be (trAssets.headOption)

    lastSection.roadNumber should be (1)
    lastSection.roadPartNumber should be (1)
    lastSection.startAddressMValue should be (70)
    lastSection.endAddressMValue should be (Some(500))
    lastTrAsset should be (trAssets.lastOption)
  }

  test("Split road address in left side with one sign"){
    val roadNumber = 1
    val roadPart = 1
    val startAddressMValue = 0
    val endAddressMValue = 500
    val starAddress = 0
    val endAddress = 500
    val trAssets = Seq(TierekisteriTrafficSignData(roadNumber, roadPart, roadPart, Track.LeftSide, 40, 40, RoadSide.Left, TRTrafficSignType.SpeedLimit, "80"))
    val roadAddress = ViiteRoadAddress(1L, roadNumber, roadPart, Track.LeftSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)

    val sections = speedLimitsTierekisteriImporter.testSplitRoadAddressSectionBySigns(trAssets, roadAddress, RoadSide.Left)

    sections.size should be (2)
    val (firstSection, firstTrAsset) = sections.head
    val (lastSection, lastTrAsset) = sections.last

    firstSection.roadNumber should be (1)
    firstSection.roadPartNumber should be (1)
    firstSection.startAddressMValue should be (40)
    firstSection.endAddressMValue should be (Some(500))
    firstTrAsset should be (None)

    lastSection.roadNumber should be (1)
    lastSection.roadPartNumber should be (1)
    lastSection.startAddressMValue should be (0)
    lastSection.endAddressMValue should be (Some(40))
    lastTrAsset should be (trAssets.headOption)
  }

  test("Split road address in left side with two sign"){
    val roadNumber = 1
    val roadPart = 1
    val startAddressMValue = 0
    val endAddressMValue = 500
    val starAddress = 0
    val endAddress = 500
    val trAssets = Seq(
      TierekisteriTrafficSignData(roadNumber, roadPart, roadPart, Track.LeftSide, 40, 40, RoadSide.Left, TRTrafficSignType.SpeedLimit, "80"),
      TierekisteriTrafficSignData(roadNumber, roadPart, roadPart, Track.LeftSide, 70, 70, RoadSide.Left, TRTrafficSignType.SpeedLimit, "90")
    )
    val roadAddress = ViiteRoadAddress(1L, roadNumber, roadPart, Track.LeftSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)

    val sections = speedLimitsTierekisteriImporter.testSplitRoadAddressSectionBySigns(trAssets, roadAddress, RoadSide.Left)

    sections.size should be (3)
    val (firstSection, firstTrAsset) = sections.head
    val (middleSection, middleTrAsset) = sections.tail.head
    val (lastSection, lastTrAsset) = sections.tail.last

    firstSection.roadNumber should be (1)
    firstSection.roadPartNumber should be (1)
    firstSection.startAddressMValue should be (70)
    firstSection.endAddressMValue should be (Some(500))
    firstTrAsset should be (None)

    middleSection.roadNumber should be (1)
    middleSection.roadPartNumber should be (1)
    middleSection.startAddressMValue should be (40)
    middleSection.endAddressMValue should be (Some(70))
    middleTrAsset should be (trAssets.lastOption)

    lastSection.roadNumber should be (1)
    lastSection.roadPartNumber should be (1)
    lastSection.startAddressMValue should be (0)
    lastSection.endAddressMValue should be (Some(40))
    lastTrAsset should be (trAssets.headOption)
  }

  test("import assets (speed limit) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testSpeedLimit = new TestSpeedLimitAssetOperations
      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val assetValue = 100
      val roadSide = RoadSide.Left

      val tr = TierekisteriSpeedLimitData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, assetValue, roadSide)
      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1.5, 11.4, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)

      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq())
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRSpeedLimitAssetClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinksAndComplementary(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.getVVHRoadLinksF(any[Int])).thenReturn(Seq(vvhRoadLink))

      testSpeedLimit.importAssets()
      val asset = speedLimitDao.getCurrentSpeedLimitsByLinkIds(Some(Set(5001))).head

      asset.linkId should be (5001)
      asset.value should be (Some(NumericValue(assetValue)))
    }
  }

  test("update assets (speed limit) from TR to OTH"){
    TestTransactions.runWithRollback() {

      val testSpeedLimit = new TestSpeedLimitAssetOperations

      val roadNumber = 4L
      val startRoadPartNumber = 200L
      val endRoadPartNumber = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val assetValue = 100
      val roadSide = RoadSide.Left

      val assetValueHist = 120
      val endAddressMValueHist = 200L

      val tr = TierekisteriSpeedLimitData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, assetValue, roadSide)
      val trHist = TierekisteriSpeedLimitData(roadNumber, startRoadPartNumber, endRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValueHist, assetValueHist, roadSide)

      val ra = ViiteRoadAddress(1L, roadNumber, startRoadPartNumber, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, 1.5, 11.4, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)

      when(mockAssetDao.expireAssetByTypeAndLinkId(any[Long], any[Seq[Long]])).thenCallRealMethod()
      when(mockMunicipalityDao.getMunicipalities).thenReturn(Seq(235))
      when(mockRoadAddressDAO.getRoadNumbers()).thenReturn(Seq(roadNumber))
      when(mockTRSpeedLimitAssetClient.fetchActiveAssetData(any[Long])).thenReturn(Seq(tr))
      when(mockTRSpeedLimitAssetClient.fetchHistoryAssetData(any[Long], any[Option[DateTime]])).thenReturn(Seq(trHist))  /*needed for update*/
      when(mockTRSpeedLimitAssetClient.fetchActiveAssetData(any[Long], any[Long])).thenReturn(Seq(trHist))

      when(mockRoadAddressDAO.withRoadAddressSinglePart(any[Long], any[Long], any[Int], any[Long], any[Option[Long]], any[Option[Int]])(any[String])).thenReturn("")
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(Seq(ra))
      when(mockRoadLinkService.getVVHRoadLinksF(any[Int])).thenReturn(Seq(vvhRoadLink))

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockVVHRoadLinkClient.fetchByLinkIds(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(Seq(vvhRoadLink))
      when(mockRoadLinkService.fetchVVHRoadlinksAndComplementary(any[Set[Long]])).thenReturn(Seq(vvhRoadLink))

      testSpeedLimit.importAssets()
      val assetI = speedLimitDao.getCurrentSpeedLimitsByLinkIds(Some(Set(5001))).head

      testSpeedLimit.updateAssets(DateTime.now())
      val assetU = speedLimitDao.getCurrentSpeedLimitsByLinkIds(Some(Set(5001))).head

      assetU.startMeasure should not be (assetI.startMeasure)
      assetU.endMeasure should be (assetI.endMeasure)
      assetU.value should be (Some(NumericValue(assetValueHist)))
    }
  }

  test("Create asset with urban area information with value 9 (speed limt = 80) "){
    TestTransactions.runWithRollback() {
      val testTRSpeedLimit = new TestSpeedLimitsTierekisteriImporter

      val roadNumber = 4L
      val roadPart = 200L
      val startAddress = 0L
      val endAddress = 250L
      val assetValue = "9"

      val trUrbanArea = TierekisteriUrbanAreaData(roadNumber, roadPart, roadPart, Track.RightSide, startAddress, endAddress, assetValue)
      val addressSection = AddressSection(roadNumber, roadPart, Track.RightSide, startAddress, Some(endAddress))

      val ra = ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, startAddress, endAddress, None, None, 5001, 1.5, 11.4, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)

      val predictedMeasures = testTRSpeedLimit.calculateMeasuresTest(ra, addressSection).head
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)

      testTRSpeedLimit.createUrbanTrafficSign(Some(vvhRoadLink),Seq(trUrbanArea), addressSection, ra, RoadSide.Left)

      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(310, Seq(5001), LinearAssetTypes.numericValuePropertyId)
      asset.size should be (1)
      asset.head.linkId should be(5001)
      asset.head.sideCode should be(SideCode.AgainstDigitizing.value)
      asset.head.value should be(Some(NumericValue(80)))
      asset.head.startMeasure should be(predictedMeasures.startMeasure +- 0.01)
      asset.head.endMeasure should be(predictedMeasures.endMeasure +- 0.01)
      asset.head.createdBy should be(Some("batch_process_stateSpeedLimit"))
    }
  }

  test("Create asset with urban area information with value different of 9 (speed limt = 50)"){
    TestTransactions.runWithRollback() {
      val testTRSpeedLimit = new TestSpeedLimitsTierekisteriImporter

      val roadNumber = 4L
      val roadPart = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val starAddress= 0L
      val endAddress = 250L
      val assetValue = "2"
      val roadSide = RoadSide.Left

      val trUrbanArea = TierekisteriUrbanAreaData(roadNumber, roadPart, roadPart, Track.RightSide, starAddress, endAddress, assetValue)
      val addressSection = AddressSection(roadNumber, roadPart, Track.RightSide, startAddressMValue, Some(endAddressMValue))

      val ra = ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, starAddress, endAddress, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)

      val predictedMeasures = testTRSpeedLimit.calculateMeasuresTest(ra, addressSection).head

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)

      testTRSpeedLimit.createUrbanTrafficSign(Some(vvhRoadLink),Seq(trUrbanArea), addressSection, ra, RoadSide.Right)

      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(310, Seq(5001), LinearAssetTypes.numericValuePropertyId)
      asset.size should be (1)
      asset.head.linkId should be(5001)
      asset.head.sideCode should be(SideCode.AgainstDigitizing.value)
      asset.head.value should be(Some(NumericValue(50)))
      asset.head.startMeasure should be(predictedMeasures.startMeasure +- 0.01)
      asset.head.endMeasure should be(predictedMeasures.endMeasure +- 0.01)
      asset.head.createdBy should be(Some("batch_process_stateSpeedLimit"))
    }
  }

  test("Spliting in two sections and creation of two assets (with and without TrUrbanArea information)") {
    TestTransactions.runWithRollback() {
      val testTRSpeedLimit = new TestSpeedLimitsTierekisteriImporter

      val roadNumber = 4L
      val roadPart = 200L
      val startAddressMValue = 0L
      val endAddressMValue = 250L
      val starAddress= 0L
      val endAddress = 250L
      val assetValue = "2"

      val trUrbanArea = TierekisteriUrbanAreaData(roadNumber, roadPart, roadPart, Track.RightSide, 230, 250, assetValue)
      val addressSection = AddressSection(roadNumber, roadPart, Track.RightSide, startAddressMValue, Some(endAddressMValue))

      val ra = ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      testTRSpeedLimit.createUrbanTrafficSign(Some(vvhRoadLink),Seq(trUrbanArea), addressSection, ra, RoadSide.Right)

      val assets = linearAssetDao.fetchLinearAssetsByLinkIds(310, Seq(5001), LinearAssetTypes.numericValuePropertyId)
      assets.size should be (2)
      assets.foreach{ asset =>
        asset.linkId should be(5001)
        asset.sideCode should be(SideCode.TowardsDigitizing.value)
        asset.createdBy should be(Some("batch_process_stateSpeedLimit"))
      }
      assets.sortBy(_.startMeasure)
      assets.head.value should be (Some(NumericValue(80)))
      assets.last.value should be (Some(NumericValue(50)))

    }
  }

  test("Creation of one assets (without TrUrbanArea information)"){
    TestTransactions.runWithRollback() {
      val testTRSpeedLimit = new TestSpeedLimitsTierekisteriImporter

      val roadNumber = 4L
      val roadPart = 200L
      val addressMValue = 10L
      val assetValue = "2"
      val startAddress = 0L
      val endAddress = 250L
      val startAddressMValue = 0
      val endAddressMValue = 250

      val addressSection = AddressSection(roadNumber, roadPart, Track.RightSide, startAddress, Some(endAddress))

      val ra = ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001, startAddress, endAddress, SideCode.AgainstDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      testTRSpeedLimit.createUrbanTrafficSign(Some(vvhRoadLink),Seq.empty[TierekisteriUrbanAreaData], addressSection, ra, RoadSide.Right)

      val asset = linearAssetDao.fetchLinearAssetsByLinkIds(310, Seq(5001), LinearAssetTypes.numericValuePropertyId)
      asset.size should be (1)
      asset.head.linkId should be(5001)
      asset.head.sideCode should be(SideCode.AgainstDigitizing.value)
      asset.head.createdBy should be(Some("batch_process_stateSpeedLimit"))
      asset.head.value should be (Some(NumericValue(80)))
    }
  }

  test("Create SpeedLimit using 2 traffic signs from 1 urban area asset, for 3 sections"){
    TestTransactions.runWithRollback() {
      val testTRSpeedLimit = new TestSpeedLimitsTierekisteriImporter

      val roadNumber = 1
      val roadPart = 1
      val startAddressMValue = 0
      val endAddressMValue = 500
      val starAddress = 0
      val endAddress = 500
      val assetValue = "2"
      val roadSide = RoadSide.Left

      val trUrbanArea = Seq(
        TierekisteriUrbanAreaData(roadNumber, roadPart, roadPart, Track.RightSide, 40, 70, assetValue))
      val addressSection = AddressSection(roadNumber, roadPart, Track.RightSide, startAddressMValue, Some(endAddressMValue))

      val ra = ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, startAddressMValue, endAddressMValue, None, None, 5001,starAddress, endAddress, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None)
      val vvhRoadLink = VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface)

      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      testTRSpeedLimit.createUrbanTrafficSign(Some(vvhRoadLink), trUrbanArea, addressSection, ra, RoadSide.Right)
      val assets = linearAssetDao.fetchLinearAssetsByLinkIds(310, Seq(5001), LinearAssetTypes.numericValuePropertyId)

      assets.size should be (3)
      assets.foreach{ asset =>
        asset.linkId should be(5001)
        asset.sideCode should be(2)
        asset.createdBy should be(Some("batch_process_stateSpeedLimit"))
      }
      assets.sortBy(_.startMeasure).map(_.value) should be (Seq(Some(NumericValue(80)), Some(NumericValue(50)), Some(NumericValue(80))))
    }
  }

  test("Create SpeedLimit based on Telematic type without roadLink type info"){
    TestTransactions.runWithRollback() {
      val testTRSpeedLimit = new TestSpeedLimitsTierekisteriImporter

      val roadNumber = 1
      val roadPart = 1

      val ra = Seq(ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, 0, 170, None, None, 5001,0, 170.3, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None),
      ViiteRoadAddress(1L, roadNumber, roadPart, Track.RightSide, 170, 175, None, None, 5002, 0, 5.8, SideCode.TowardsDigitizing, false, Seq(), false, None, None, None))

      val vvhRoadLink = Seq(VVHRoadlink(5001, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface),
      VVHRoadlink(5002, 235, Nil, State, TrafficDirection.UnknownDirection, FeatureClass.AllOthers, None, Map(), ConstructionType.InUse, LinkGeomSource.NormalLinkInterface))

      val startMValue = 118
      val trAsset =  Seq(TierekisteriTrafficSignData(roadNumber, roadPart, roadPart, Track.RightSide, startMValue, startMValue, RoadSide.Right, TRTrafficSignType.TelematicSpeedLimit,""))
      val mappedLinkType: Map[Long, Seq[(Long, LinkType)]] = Map((5001L, Seq((5001L, Motorway))))
      when(mockVVHClient.roadLinkData).thenReturn(mockVVHRoadLinkClient)
      when(mockRoadAddressDAO.getRoadAddress(any[String => String].apply)).thenReturn(ra)
      when(mockRoadLinkService.fetchVVHRoadlinks(any[Set[Long]], any[Boolean])).thenReturn(vvhRoadLink)
      when(mockRoadLinkService.getAllLinkType(any[Seq[Long]])).thenReturn(mappedLinkType)
      testTRSpeedLimit.generateOneSideSpeedLimitsTest(roadNumber, RoadSide.Right, trAsset, Seq(), ra)

      val assets = linearAssetDao.fetchLinearAssetsByLinkIds(310, Seq(5001, 5002), LinearAssetTypes.numericValuePropertyId)

      assets.size should be (3)
      assets.foreach{ asset =>
        asset.sideCode should be(2)
        asset.createdBy should be(Some("batch_process_stateSpeedLimit"))
      }
      assets.sortBy(_.linkId).map(_.linkId) should be (Seq(5001, 5001, 5002))
      assets.sortBy(_.linkId).sortBy(_.startMeasure).map(_.value) should be (Seq(Some(NumericValue(80)), Some(NumericValue(120)), Some(NumericValue(120))))
    }
  }
}
