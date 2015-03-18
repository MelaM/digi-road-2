package fi.liikennevirasto.digiroad2

import _root_.oracle.spatial.geometry.JGeometry
import fi.liikennevirasto.digiroad2.asset.{ValidityPeriod, BoundingRectangle}
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.user.User
import org.joda.time.{Interval, DateTime, LocalDate}
import scala.slick.driver.JdbcDriver.backend.Database
import scala.slick.driver.JdbcDriver.backend.Database.dynamicSession
import scala.slick.jdbc.{PositionedResult, GetResult}
import scala.slick.jdbc.StaticQuery.interpolation
import fi.liikennevirasto.digiroad2.asset.oracle.Queries.getPoint

object MassTransitStopService {
  case class MassTransitStop(id: Long, nationalId: Long, lon: Double, lat: Double, bearing: Option[Int],
                             validityDirection: Int, readOnly: Boolean, municipalityNumber: Int,
                             validityPeriod: String, floating: Boolean)

  private implicit val getLocalDate = new GetResult[Option[LocalDate]] {
    def apply(r: PositionedResult) = {
      r.nextDateOption().map(new LocalDate(_))
    }
  }

  private def validityPeriod(validFrom: Option[LocalDate], validTo: Option[LocalDate]): String = {
    (validFrom, validTo) match {
      case (Some(from), None) => if (from.isBefore(LocalDate.now())) { ValidityPeriod.Current } else { ValidityPeriod.Future }
      case (None, Some(to)) => if (to.isBefore(LocalDate.now())) { ValidityPeriod.Past } else { ValidityPeriod.Current }
      case (Some(from), Some(to)) =>
        val interval = new Interval(from.toDateMidnight, to.toDateMidnight)
        if (interval.containsNow()) { ValidityPeriod.Current }
        else if (interval.isBeforeNow) { ValidityPeriod.Past }
        else { ValidityPeriod.Future }
      case _ => ValidityPeriod.Current
    }
  }

  def getByBoundingBox: Seq[MassTransitStop] = {
    // TODO: add bounding box filtering
    // TODO: add validity period filtering
    // TODO: add authorization filtering
    // TODO: calculate floating status
    // TODO: update floating status
    Database.forDataSource(OracleDatabase.ds).withDynSession {
      val massTransitStops = sql"""
          select a.id, a.external_id, a.bearing, lrm.side_code,
          a.municipality_code, a.floating, lrm.start_measure, lrm.end_measure, lrm.mml_id,
          a.geometry, a.valid_from, a.valid_to
          from asset a
          join asset_link al on a.id = al.asset_id
          join lrm_position lrm on al.position_id = lrm.id
          where a.asset_type_id = 10
       """.as[(Long, Long, Option[Int], Int, Int, Boolean, Double, Double, Long, Point, Option[LocalDate], Option[LocalDate])].list()
      massTransitStops.map { massTransitStop =>
        val (id, nationalId, bearing, sideCode, municipalityCode, floating, _, _, _, point, validFrom, validTo) = massTransitStop
        // TODO: add readOnly
        MassTransitStop(id, nationalId, point.x, point.y, bearing, sideCode, true, municipalityCode, validityPeriod(validFrom, validTo), floating)
      }
    }

/*    def andAssetWithinBoundingBox = boundingRectangle map { b =>
      val boundingBox = new JGeometry(b.leftBottom.x, b.leftBottom.y, b.rightTop.x, b.rightTop.y, 3067)
      ("AND SDO_FILTER(geometry, ?) = 'TRUE'", List(storeGeometry(boundingBox, dynamicSession.conn)))
    }
    def andValidityInRange = (validFrom, validTo) match {
      case (Some(from), Some(to)) => Some(andByValidityTimeConstraint, List(jodaToSqlDate(from), jodaToSqlDate(to)))
      case (None, Some(to)) => Some(andExpiredBefore, List(jodaToSqlDate(to)))
      case (Some(from), None) => Some(andValidAfter, List(jodaToSqlDate(from)))
      case (None, None) => None
    }
    val query = QueryCollector(allAssetsWithoutProperties).add(andValidityInRange).add(andAssetWithinBoundingBox)
    val allAssets = collectedQuery[ListedAssetRow](query).iterator
    val assetsWithProperties: Map[Long, Seq[ListedAssetRow]] = allAssets.toSeq.groupBy(_.id)
    val assetsWithRoadLinks: Map[Long, (Option[(Long, Int, Option[Point], AdministrativeClass)], Seq[ListedAssetRow])] = assetsWithProperties.mapValues { assetRows =>
      val row = assetRows.head
      val roadLinkOption = getOptionalProductionRoadLink(row)
      (roadLinkOption, assetRows)
    }
    val authorizedAssets =
      if (user.isOperator()) {
        assetsWithRoadLinks
      } else {
        assetsWithRoadLinks.filter { case (_, (roadLinkOption, assetRows)) =>
          val assetRow = assetRows.head
          user.isAuthorizedToRead(assetRow.municipalityCode)
        }
      }
    val assets = authorizedAssets.map { case (assetId, (roadLinkOption, assetRows)) =>
      val row = assetRows.head
      val point = row.point.get
      (Asset(id = row.id,
        externalId = row.externalId,
        assetTypeId = row.assetTypeId,
        lon = point.x,
        lat = point.y,
        roadLinkId = roadLinkOption.map(_._1).getOrElse(-1), // FIXME: Temporary solution for possibly missing roadLinkId
        imageIds = assetRows.map(row => getImageId(row.image)).toSeq,
        bearing = row.bearing,
        validityDirection = Some(row.validityDirection),
        municipalityNumber = row.municipalityCode,
        validityPeriod = validityPeriod(row.validFrom, row.validTo),
        floating = isFloating(row, roadLinkOption)), row.persistedFloating)
    }
    assets.foreach(updateAssetFloatingStatus)
    assets.map(_._1).toSeq*/
  }
}
