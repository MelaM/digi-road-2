package fi.liikennevirasto.digiroad2.dao

import slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import com.github.tototoshi.slick.MySQLJodaSupport._
import fi.liikennevirasto.digiroad2.asset.AutoGeneratedValues
import fi.liikennevirasto.digiroad2.oracle.MassQuery
import fi.liikennevirasto.digiroad2.service.{LatestModificationInfo, SuggestedAssetsStructure, VerificationInfo}
import org.joda.time.DateTime
import slick.jdbc.StaticQuery.interpolation

class VerificationDao {
  val TwoYears: Int = 24

  def getVerifiedInfoTypes(municipalityId: Int) : List[VerificationInfo] = {
    val verifiedAssetTypes =
      sql"""
          SELECT m.id, m.name_fi, mv.verified_by, mv.verified_date, atype.id AS typeId, atype.name AS assetName,
          (CASE WHEN MONTHS_BETWEEN(current_timestamp, mv.verified_date) < $TwoYears THEN 1 ELSE 0 END) AS verified, atype.geometry_type, mv.last_user_modification,
           mv.last_date_modification, mv.number_of_assets, mv.refresh_date, REGEXP_COUNT(mv.suggested_assets , ',') + 1 as countSuggested
           FROM municipality m
           JOIN asset_type atype ON atype.verifiable = 1
           LEFT JOIN municipality_verification mv ON mv.municipality_id = m.id AND mv.asset_type_id = atype.id AND mv.valid_to IS NULL OR mv.valid_to > current_timestamp
           WHERE m.id = $municipalityId
           """.as[(Int, String, Option[String], Option[DateTime], Int, String, Boolean, String, Option[String], Option[DateTime], Int, Option[DateTime], Option[Int])].list
    verifiedAssetTypes.map { case ( municipalityCode, municipalityName, verifiedBy, verifiedDate, assetTypeCode, assetTypeName, verified, geometryType, lastUserModification, lastDateModification, numberOfAsset, refreshDate, suggestedAssetsCount) =>
      VerificationInfo(municipalityCode, municipalityName, assetTypeCode, assetTypeName, verifiedBy, verifiedDate, geometryType, numberOfAsset, verified, lastUserModification, lastDateModification, refreshDate, suggestedAssetsCount)
    }
  }

  def getVerifiedAssetTypes : List[(Int, String)] = {
    sql"""SELECT id, geometry_type FROM asset_type atype WHERE atype.verifiable = 1""".as[(Int, String)].list
  }

  def getSuggestedByTypeIdAndMunicipality(municipalityId: Int, typeId: Int): SuggestedAssetsStructure = {
    val suggestedAssets =
      sql"""
        select m.name_fi, m.id, atype.name, mv.suggested_assets
        from municipality_verification mv
        join asset_type atype on atype.id = mv.asset_type_id
        join municipality m on m.id = mv.municipality_id
        where m.id = $municipalityId and atype.id = $typeId and valid_to is null
      """.as[(String, Int, String, String)].list
    suggestedAssets.map { row =>
      SuggestedAssetsStructure(row._1, row._2, row._3, typeId, row._4.split(",").map(_.toInt).toSet)
    }.head
  }

  def getNumberOfPointAssets(municipalityId: Int) : Seq[(Int, Int)] = {
      sql"""
           Select a.asset_type_id , count(a.id)
                from asset_type atype
                JOIN asset a ON a.ASSET_TYPE_ID = atype.ID and (a.valid_to IS NULL OR a.valid_to > current_timestamp )
                join property p on p.asset_type_id = a.asset_type_id and p.property_type = 'checkbox' and p.public_id = 'suggest_box'
                join multiple_choice_value mcv on mcv.asset_id = a.id and mcv.property_id = p.id
                join enumerated_value ev on ev.property_id = p.id and ev.value != 1 AND ev.id = mcv.ENUMERATED_VALUE_ID
                WHERE a.municipality_code = $municipalityId
                and atype.verifiable = 1
                and atype.GEOMETRY_TYPE = 'point'
                GROUP BY a.asset_type_id """.as[(Int, Int)].list
  }

  def getLastModificationPointAssets(municipalityId: Int) : Seq[LatestModificationInfo] = {
    val lastModification =
      sql"""
        select typeId, modified_date, modified_by
        from  (
          select atype.id AS typeId, a.modified_date, a.modified_by,
          ROW_NUMBER () OVER (PARTITION BY a.asset_type_id ORDER BY a.modified_date desc nulls last) AS rownumber
          from asset_type atype
          JOIN asset a ON a.ASSET_TYPE_ID = atype.ID  AND (a.VALID_TO IS NULL OR a.valid_to > current_timestamp)
          where atype.verifiable = 1
          and a.municipality_code = $municipalityId
          and atype.GEOMETRY_TYPE = 'point'
          and a.modified_by not in ('#${AutoGeneratedValues.allAutoGeneratedValues.mkString("','")}')
        ) tb
        where tb.rownumber = 1""".as[(Int, Option[DateTime], Option[String])].list
    lastModification.map { case ( typeId, modifiedDate, modifiedBy) =>
      LatestModificationInfo(typeId, modifiedBy, modifiedDate)
    }
  }

  def getLastModificationLinearAssets(ids: Set[Long]) : Seq[LatestModificationInfo] = {
    val lastModification = MassQuery.withIds(ids) { idTableName =>
      sql"""
      select typeId, modified_date, modified_by
      from (
        SELECT a.asset_type_id AS typeId, a.modified_date, a.modified_by,
        ROW_NUMBER () OVER (PARTITION BY a.asset_type_id ORDER BY a.modified_date desc nulls last) AS rownumber
        from asset_type atype
        JOIN asset a ON a.ASSET_TYPE_ID = atype.ID  AND a.VALID_TO IS NULL
        join asset_link al on a.id = al.asset_id
        join lrm_position lrm on lrm.id = al.position_id
        left join property p on p.asset_type_id = a.asset_type_id and p.property_type = 'checkbox' and p.public_id = 'suggest_box'
        left join multiple_choice_value mcv on mcv.asset_id = a.id and mcv.property_id = p.id
        left join enumerated_value ev on ev.property_id = p.id and ev.value != 1 AND ev.id = mcv.ENUMERATED_VALUE_ID
        where atype.verifiable = 1
        and lrm.link_id in (select id from #$idTableName)
        and (a.modified_by not in ('#${AutoGeneratedValues.allAutoGeneratedValues.mkString("','")}') or a.modified_by is null)
      ) tb
      where tb.rownumber = 1 """.as[(Int, Option[DateTime], Option[String])].list
    }
    lastModification.map { case ( typeId, modifiedDate, modifiedBy) =>
      LatestModificationInfo(typeId, modifiedBy, modifiedDate)
    }
  }

  def getSuggestedLinearAssets(ids: Set[Long]): Seq[(Long, Int)] = {
    MassQuery.withIds(ids) { idTableName =>
      sql"""
      select a.id, a.asset_type_id
        from asset a
        join asset_type atype on atype.geometry_type = 'linear' and atype.id = a.asset_type_id
        join asset_link al on a.id = al.asset_id
        join lrm_position lrm on lrm.id = al.position_id
        join property p on p.asset_type_id = a.asset_type_id and p.property_type = 'checkbox' and p.public_id = 'suggest_box'
        join multiple_choice_value mcv on mcv.asset_id = a.id and mcv.property_id = p.id
        join enumerated_value ev on ev.property_id = p.id and ev.value = 1 and ev.id = mcv.enumerated_value_id and  lrm.link_id in (select id from #$idTableName)
        where (a.valid_to IS NULL OR a.valid_to > current_timestamp)
        """.as[(Long, Int)].list
    }
  }

  def getSuggestedPointAssets(municipalityCode: Int): Seq[(Long, Int)] = {
    sql"""
    select a.id, a.asset_type_id
      from asset a
      join asset_type atype on atype.geometry_type = 'point' and atype.id = a.asset_type_id
      join property p on p.asset_type_id = a.asset_type_id and p.property_type = 'checkbox' and p.public_id = 'suggest_box'
      join multiple_choice_value mcv on mcv.asset_id = a.id and mcv.property_id = p.id
      join enumerated_value ev on ev.property_id = p.id and ev.value = 1 and ev.id = mcv.enumerated_value_id
      where a.municipality_code = #$municipalityCode and (a.valid_to IS NULL OR a.valid_to > current_timestamp)
    """.as[(Long, Int)].list
  }

  def getAssetVerification(municipalityCode: Int, assetTypeCode: Int): Seq[VerificationInfo] = {
    val verifiedAssetType =
      sql"""
          SELECT m.id, m.name_fi, mv.verified_by, mv.verified_date, atype.id AS assetId, atype.name AS assetName,
                (CASE
                    WHEN MONTHS_BETWEEN(current_timestamp, mv.verified_date) < $TwoYears
                      THEN 1
                      ELSE 0
                END) AS verified, atype.GEOMETRY_TYPE, mv.number_of_assets AS counting
                FROM municipality m
                JOIN asset_type atype ON atype.id = $assetTypeCode
                LEFT JOIN municipality_verification mv ON mv.municipality_id = m.id and mv.valid_to is null AND mv.asset_type_id = atype.id
                WHERE  m.id = $municipalityCode
        """.as[(Int, String, Option[String], Option[DateTime], Int, String, Boolean, String, Int)].list

    verifiedAssetType.map { case (municipality, municipalityName, verifiedBy, verifiedDate, assetType, assetTypeName, verified, geometryType, counter) =>
      VerificationInfo(municipality, municipalityName, assetType, assetTypeName, verifiedBy, verifiedDate, geometryType, counter, verified, None)
    }
  }

  def getCriticalAssetVerification(municipalityId: Int, assetTypeCodes: Seq[Int]): Seq[VerificationInfo] = {
    val criticalAssetTypes =
      sql"""
          SELECT m.id, m.name_fi, mv.verified_by, mv.verified_date, atype.id AS assetId, atype.name AS assetName, atype.geometry_type, mv.number_of_assets, mv.refresh_date
          FROM municipality m
          JOIN asset_type atype ON atype.verifiable = 1
          LEFT JOIN municipality_verification mv ON mv.municipality_id = m.id AND mv.asset_type_id = atype.id AND mv.valid_to IS NULL
          WHERE m.id = $municipalityId
          AND atype.id IN (#${assetTypeCodes.mkString(",")})""".as[(Int, String, Option[String], Option[DateTime], Int, String, String, Int, Option[DateTime])].list
    criticalAssetTypes.map { case ( municipalityCode, municipalityName, verifiedBy, verifiedDate, assetTypeCode, assetTypeName, geometryType, counter, refreshDate) =>
      VerificationInfo(municipalityCode, municipalityName, assetTypeCode, assetTypeName, verifiedBy, verifiedDate, geometryType, counter, refreshDate = refreshDate)
    }
  }

  def getVerificationInfo(municipalityId: Int, assetTypeIds: Set[Int]): Seq[(Int, Int, Option[String], Option[DateTime], Int, Option[DateTime], String)] = {
    val filterByType = if(assetTypeIds.nonEmpty) s"and asset_type_id in (${assetTypeIds.mkString(",")})" else ""
    sql"""
       SELECT mv.id, mv.asset_type_id, mv.last_user_modification, mv.last_date_modification, mv.number_of_assets, mv.refresh_date, mv.suggested_assets
         FROM municipality_verification mv
         WHERE municipality_Id = $municipalityId
         and valid_to is null
         #$filterByType
         """.as[(Int, Int, Option[String], Option[DateTime], Int, Option[DateTime], String)].list
  }

  def insertAssetTypeVerification(municipalityId: Int, assetTypeId: Int, username: String): Long = {
    val id = sql"""select nextval('primary_key_seq')""".as[Long].first
    sqlu"""insert into municipality_verification (id, municipality_id, asset_type_id, verified_date, verified_by)
           values ($id, $municipalityId, $assetTypeId, current_timestamp, $username)
      """.execute
    id
  }
  def insertAssetTypeVerification(municipalityId: Int, assetTypeId: Int, verifiedBy: Option[String], lastUserModification: Option[String], lastDateModification: Option[DateTime], numberOfAsset: Int, refreshDate: Option[DateTime], suggestedAssets: String): Long = {
    val verifiedDate = if(verifiedBy.nonEmpty) "current_timestamp" else "null"
    val id = sql"""select nextval('primary_key_seq')""".as[Long].first
    sqlu"""insert into municipality_verification (id, municipality_id, asset_type_id, verified_by, verified_date, last_user_modification, last_date_modification, number_of_assets, refresh_date, suggested_assets)
           values ($id, $municipalityId, $assetTypeId, $verifiedBy, #$verifiedDate, $lastUserModification, $lastDateModification, $numberOfAsset, $refreshDate, $suggestedAssets)
      """.execute
    id
  }

  def updateAssetTypeVerification(municipalityId: Int, assetTypeId: Int, lastUserModification: Option[String], lastDateModification: Option[DateTime], numberOfAsset: Int, refreshDate: Option[DateTime], suggestedAssets: String): Unit = {
    sqlu"""update municipality_verification
           set last_user_modification = $lastUserModification,
               last_date_modification = $lastDateModification,
               number_of_assets = $numberOfAsset,
               refresh_date = $refreshDate,
               suggested_assets = $suggestedAssets
         where municipality_id = $municipalityId
           and asset_type_id = $assetTypeId
           and valid_to is null
      """.execute
  }

  def expireAssetTypeVerification(municipalityCode: Int, assetTypeCode: Int, userName: String) = {
    sqlu"""update municipality_verification mv
           set valid_to = current_timestamp, modified_by = $userName
           where mv.municipality_id = $municipalityCode
           and mv.asset_type_id = $assetTypeCode
           and valid_to is null
      """.execute
    assetTypeCode
  }

  def getVerifiableAssetTypes: Seq[Int] = {
    sql"""select asst.id
           from asset_type asst
           where asst.verifiable = 1
      """.as[(Int)].list
  }

  def getModifiedAssetTypes(linkIds : Set[Long]) : List[LatestModificationInfo] = {
    val modifiedAssetTypes = MassQuery.withIds(linkIds) { idTableName =>
      sql"""
           select assetTypeId, modifiedBy, modifiedDate
           from (
              select a.asset_type_id as assetTypeId, a.modified_by as modifiedBy, max(TO_DATE(TO_CHAR( a.modified_date, 'YYYY-MM-DD'), 'YYYY-MM-DD hh24:mi:ss')) as modifiedDate
              from asset a
              join asset_link al on a.id = al.asset_id
              join lrm_position lrm on lrm.id = al.position_id
              where a.modified_date is not null
              and a.valid_to is null
              and a.modified_by not in ('#${AutoGeneratedValues.allAutoGeneratedValues.mkString("','")}')
              and lrm.link_id in (select id from #$idTableName)
              group by a.asset_type_id, a.modified_by
              order by max(a.modified_date) desc, a.asset_type_id, a.modified_by
              ) limit 4""".as[(Int, Option[String], Option[DateTime])].list
      }
    modifiedAssetTypes.map { case (assetTypeCode, modifiedBy, modifiedDate) =>
      LatestModificationInfo(assetTypeCode,  modifiedBy, modifiedDate)
    }
  }

  def getNumberSuggestedAssetNumber(municipalityCode: Set[Int]) : Long = {
    sql"""
      select SUM(REGEXP_COUNT(suggested_assets , ',') + 1)
      from municipality_verification
      where valid_to is null
      and municipality_id in (#${municipalityCode.mkString(",")})
          """.as[Long].first
  }

  def getDashboardInfo(municipalityCodes: Set[Int]): List[LatestModificationInfo] = {
    val modifiedAssets = sql"""
        select assetTypeId, modifiedBy, modifiedDate
        from (
          select db.asset_type_id as assetTypeId, db.modified_by as modifiedBy, max(to_date(to_char( db.last_modified_date, 'yyyy-mm-dd'), 'yyyy-mm-dd hh24:mi:ss')) as modifiedDate
          from dashboard_info db
          where db.municipality_id in (#${municipalityCodes.mkString(",")})
          group by db.asset_type_id, db.modified_by, db.municipality_id
          order by max(db.last_modified_date) desc, db.asset_type_id, db.modified_by
        ) where limit 4""".as[(Int, Option[String], Option[DateTime])].list

    modifiedAssets.map { case (assetTypeCode, modifiedBy, modifiedDate) =>
      LatestModificationInfo(assetTypeCode,  modifiedBy, modifiedDate)
    }
  }

  def insertAssetModified(municipalityCode: Int, latestModificationInfo: LatestModificationInfo): Unit = {
    sqlu"""
      insert into dashboard_info (municipality_id, asset_type_id, modified_by, last_modified_date)
      values ($municipalityCode, ${latestModificationInfo.assetTypeCode}, ${latestModificationInfo.modifiedBy}, ${latestModificationInfo.modifiedDate})
    """.execute
  }
}
