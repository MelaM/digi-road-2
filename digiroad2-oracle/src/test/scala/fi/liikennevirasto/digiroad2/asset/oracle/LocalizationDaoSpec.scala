package fi.liikennevirasto.digiroad2.asset.oracle

import org.scalatest.{Tag, BeforeAndAfter, Matchers, FunSuite}
import fi.liikennevirasto.digiroad2.asset.LocalizedString
import LocalizedString._
import scala.slick.driver.JdbcDriver.backend.Database
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.asset.oracle.LocalizationDao._

class LocalizationDaoSpec extends FunSuite with Matchers with BeforeAndAfter {
  test("insert localized string", Tag("db")) {
    Database.forDataSource(OracleDatabase.ds).withDynTransaction {
      val ls = insertLocalizedString(new LocalizedString(values = Map(LangFi -> "testi", LangSv -> "test")))
      ls.id should not be empty
    }
  }

  test("retrieve localized string", Tag("db")) {
    Database.forDataSource(OracleDatabase.ds).withDynTransaction {
      val ls = insertLocalizedString(new LocalizedString(values = Map(LangFi -> "suomeksi", LangSv -> "på svenska")))
      val lsPersistent = getLocalizedString(ls.id.get).get
      lsPersistent.id should be (ls.id)
      lsPersistent.forLanguage(LangFi) should be (Some("suomeksi"))
      lsPersistent.forLanguage(LangSv) should be (Some("på svenska"))
    }
  }

  test("update localized string", Tag("db")) {
    Database.forDataSource(OracleDatabase.ds).withDynTransaction {
      val ls = insertLocalizedString(new LocalizedString(values = Map(LangFi -> "suomeksi", LangSv -> "på svenska")))
      updateLocalizedString(ls.copy(values = Map(LangFi -> "päivitetty", LangSv -> "uppdaterad")))
      val lsUpdated = getLocalizedString(ls.id.get).get
      lsUpdated.forLanguage(LangFi) should be (Some("päivitetty"))
      lsUpdated.forLanguage(LangSv) should be (Some("uppdaterad"))
    }
  }
}
