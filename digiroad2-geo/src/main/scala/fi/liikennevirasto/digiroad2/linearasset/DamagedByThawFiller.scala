package fi.liikennevirasto.digiroad2.linearasset

import java.text.SimpleDateFormat

import fi.liikennevirasto.digiroad2.asset._
import fi.liikennevirasto.digiroad2.linearasset.LinearAssetFiller.{ChangeSet, ValueAdjustment}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class DamagedByThawFiller extends AssetFiller {
  val ActivePeriod = "spring_thaw_period"
  val Repetition = "annual_repetition"
  private val dateFormat = "dd.MM.yyyy"
  private val formatter = DateTimeFormat.forPattern(dateFormat)
  private val today = DateTime.now()

  override protected def updateValues(roadLink: RoadLink, assets: Seq[PersistedLinearAsset], changeSet: ChangeSet): (Seq[PersistedLinearAsset], ChangeSet) = {

    def getProperties(publicId: String, propertyData: Seq[DynamicProperty]): Seq[DynamicPropertyValue] = {
      propertyData.find(p => p.publicId == publicId) match {
        case Some(props) => props.values
        case _ => Seq()
      }
    }

    def dateToString(date: DateTime): String = {
      date.toString(dateFormat)
    }

    def stringToDate(date: String): DateTime = {
      formatter.parseDateTime(date)
    }

    def toCurrentYear(period: DatePeriodValue): DatePeriodValue = {
      val endDate = stringToDate(period.endDate)
      val startDate = stringToDate(period.startDate)
      val difference = today.getYear - endDate.getYear
      if(difference == 0)
        DatePeriodValue(dateToString(startDate.plusYears(1)), dateToString(endDate.plusYears(1)))
      else
        DatePeriodValue(dateToString(startDate.plusYears(difference)), dateToString(endDate.plusYears(difference)))
    }

    def outsidePeriod(value: DynamicPropertyValue): Boolean = {
      val period = DatePeriodValue.fromMap(value.value.asInstanceOf[Map[String, String]])
      val endDate = stringToDate(period.endDate)
      val thisYear = today.getYear
      val endDateYear = endDate.getYear

      thisYear - endDateYear >= 0 && endDate.isBefore(today)
    }

    def isRepeated(checkbox: Seq[DynamicPropertyValue]): Boolean = {
      checkbox.exists(x => x.value.asInstanceOf[String].equals("1"))
    }

    def needUpdates(properties: Seq[DynamicProperty]): Boolean = {
      isRepeated(getProperties(Repetition, properties)) &&
        getProperties(ActivePeriod, properties).exists { period =>
          outsidePeriod(period)
        }
    }

    val (toUpdate, noneNeeded) = assets.partition( asset =>
      asset.value.map(_.asInstanceOf[DynamicValue].value.properties).exists {
        propertyData => needUpdates(propertyData)
      }
    )

    val adjustedAssets = toUpdate.map { asset =>
      asset.copy(value = Some(DynamicValue(DynamicAssetValue(asset.value.get.asInstanceOf[DynamicValue].value.properties.map { prop =>
          if (prop.publicId == ActivePeriod) {
            prop.copy(values = prop.values.map { period =>
              if(outsidePeriod(period))
                DynamicPropertyValue(DatePeriodValue.toMap(toCurrentYear(DatePeriodValue.fromMap(period.value.asInstanceOf[Map[String, String]]))))
              else
                period
            })
          } else prop
      }))), modifiedBy = Some(AutoGeneratedValues.annualUpdate))
    }

    (adjustedAssets ++ noneNeeded, changeSet.copy(valueAdjustments = changeSet.valueAdjustments ++ adjustedAssets.map {asset => ValueAdjustment(asset)}))
  }
}
