package fi.liikennevirasto.digiroad2.util

import java.io.{ByteArrayOutputStream, BufferedInputStream}
import scala.slick.driver.JdbcDriver.backend.Database
import scala.slick.jdbc._
import scala.slick.jdbc.StaticQuery.interpolation
import Database.dynamicSession
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase._

object BusStopIconImageData {
  val imagesForBusStopTypes = Map[String, String] ("1" -> "/raitiovaunu.png", "2" -> "/paikallisliikenne.png", "3" -> "/kaukoliikenne.png", "4" -> "/pikavuoro.png", "99" -> "/pysakki_ei_tiedossa.png")

  implicit object SetByteArray extends SetParameter[Array[Byte]] {
    def apply(v: Array[Byte], pp: PositionedParameters) {
      pp.setBytes(v)
    }
  }

  def insertImages(modifier: String) {
    insertImages(modifier, "Pysäkin tyyppi", imagesForBusStopTypes)
  }

  def insertImages(modifier: String, propertyName: String, vals: Map[String, String]) {
    Database.forDataSource(ds).withDynSession {
      val propertyId = sql"""select p.id from property p, localized_string ls where p.name_localized_string_id = ls.id and ls.value_fi = ${propertyName}""".as[Long].first
      insertImagesWithPropertyId(modifier, propertyId, vals)
    }
  }

  def insertImages_2_4(modifier: String) {
   insertImages_2_4(modifier, "Pysäkin tyyppi", imagesForBusStopTypes)
  }

  def insertImages_2_4(modifier: String, propertyName: String, vals: Map[String, String]) {
    Database.forDataSource(ds).withDynSession {
      val propertyId = sql"""select id from property where name_fi = ${propertyName}""".as[Long].first
      insertImagesWithPropertyId(modifier, propertyId, vals)
    }
  }

  def insertImagesWithPropertyId(modifier: String, propertyId: Long, vals: Map[String, String]) {
    vals.foreach { keyVal =>
      val s = getClass.getResourceAsStream(keyVal._2)
      val bis = new BufferedInputStream(s)
      val fos = new ByteArrayOutputStream(65535)
      val buf = new Array[Byte](1024)
      Stream.continually(bis.read(buf)).takeWhile(_ != -1).foreach(fos.write(buf, 0, _))
      val byteArray = fos.toByteArray

      sqlu"""
          insert into image (id, created_by, modified_date, file_name, image_data)
          values (${keyVal._1}, $modifier, current_timestamp, ${keyVal._2.tail}, $byteArray)
        """.execute
      sqlu"""
          update enumerated_value set image_id = ${keyVal._1} where property_id = $propertyId and value = ${keyVal._1}
        """.execute
    }
  }
}
