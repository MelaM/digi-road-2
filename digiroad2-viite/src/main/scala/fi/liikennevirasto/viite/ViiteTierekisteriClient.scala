package fi.liikennevirasto.viite
import java.util.Properties

import fi.liikennevirasto.digiroad2.util.TierekisteriAuthPropertyReader
import fi.liikennevirasto.viite.dao._
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClientBuilder
import org.json4s.jackson.Serialization
import org.json4s.{CustomSerializer, DefaultFormats, Extraction, StreamInput}
import org.joda.time.format.DateTimeFormat
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods.parse

import scala.util.control.NonFatal
case class TRProjectStatus(id:Option[Long], trProjectId:Option[Long], trSubProjectId:Option[Long], trTrackingCode:Option[Long],
                           status:Option[String], name:Option[String], changeDate:Option[String], ely:Option[Int],
                           trModifiedDate:Option[String], user:Option[String], trPublishedDate:Option[String],
                           trJobNumber:Option[Long], errorMessage:Option[String], trProcessingStarted:Option[String],
                           trProcessingEnded:Option[String], errorCode:Option[Int])
case class ChangeProject(id:Long, name:String, user:String, ely:Long, changeDate:String, changeInfoSeq:Seq[RoadAddressChangeInfo])
case class ProjectChangeStatus(projectId: Long, status: Int, reason: String)


case object ChangeProjectSerializer extends CustomSerializer[ChangeProject](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats + ChangeInfoItemSerializer
    ChangeProject(o.values("id").asInstanceOf[BigInt].longValue(), o.values("name").asInstanceOf[String],
      o.values("user").asInstanceOf[String], o.values("ely").asInstanceOf[BigInt].intValue(),
      o.values("change_date").asInstanceOf[String],
      (o \\ "change_info").extract[Seq[RoadAddressChangeInfo]])
}, {
  case o: ChangeProject =>
    implicit val formats = DefaultFormats + ChangeInfoItemSerializer
    JObject(
      JField("id", JInt(BigInt.apply(o.id))),
      JField("name", JString(o.name)),
      JField("user", JString(o.user)),
      JField("ely", JInt(BigInt.apply(o.ely))),
      JField("change_date", JString(o.changeDate)),
      JField("change_info", Extraction.decompose(o.changeInfoSeq))
    )
}))

case object ChangeInfoItemSerializer extends CustomSerializer[RoadAddressChangeInfo](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats + ChangeInfoRoadPartsSerializer
    RoadAddressChangeInfo(ChangeType.apply(o.values("change_type").asInstanceOf[BigInt].intValue),
      (o \\ "source").extract[RoadAddressChangeRecipient], (o \\ "target").extract[RoadAddressChangeRecipient],
      Discontinuity.apply(o.values("continuity").asInstanceOf[BigInt].intValue),
      RoadType.apply(o.values("road_type").asInstanceOf[BigInt].intValue))
}, {
  case o: RoadAddressChangeInfo =>
    implicit val formats = DefaultFormats + ChangeInfoRoadPartsSerializer
    JObject(
      JField("change_type", JInt(BigInt.apply(o.changeType.value))),
      JField("continuity", JInt(BigInt.apply(o.discontinuity.value))),
      JField("road_type", JInt(BigInt.apply(o.roadType.value))),
      JField("source", Extraction.decompose(o.source)),
      JField("target", Extraction.decompose(o.target))
    )
}))

case object TRProjectStatusSerializer extends CustomSerializer[TRProjectStatus](format => ( {
  case o: JObject =>
    def jIntToLong(jInt: Any): Long = {
      jInt.asInstanceOf[BigInt].longValue()
    }
    def jIntToInt(jInt: Any): Int = {
      jInt.asInstanceOf[BigInt].intValue()
    }
    def jStringToString(jString: Any): String = {
      jString.asInstanceOf[String]
    }
    val map = o.values
    val (id, id_tr_projekti, projekti, tunnus,
    status, name, change_date, ely,
    muutospvm, user, published_date,
    job_number, error_message, start_time,
    end_time, error_code) =
      (map.get("id"), map.get("id_tr_projekti"), map.get("projekti"), map.get("tunnus"), map.get("status"), map.get("name"),
        map.get("change_date"), map.get("ely"), map.get("muutospvm"), map.get("user"), map.get("published_date"), map.get("job_number"),
        map.get("error_message"), map.get("start_time"), map.get("end_time"), map.get("error_code"))
    TRProjectStatus(id.map(jIntToLong), id_tr_projekti.map(jIntToLong), projekti.map(jIntToLong), tunnus.map(jIntToLong),
      status.map(jStringToString), name.map(jStringToString),change_date.map(jStringToString), ely.map(jIntToInt),
      muutospvm.map(jStringToString), user.map(jStringToString),published_date.map(jStringToString), job_number.map(jIntToLong),
      error_message.map(jStringToString), start_time.map(jStringToString),end_time.map(jStringToString), error_code.map(jIntToInt))
}, {
  case s: TRProjectStatus =>
    JObject(
      JField("id", s.id.map(l => JInt(BigInt.apply(l))).orNull),
      JField("id_tr_projekti", s.trProjectId.map(l => JInt(BigInt.apply(l))).orNull),
      JField("projekti", s.trSubProjectId.map(l => JInt(BigInt.apply(l))).orNull),
      JField("tunnus", s.trTrackingCode.map(l => JInt(BigInt.apply(l))).orNull),
      JField("status", s.status.map(l => JString(l)).orNull),
      JField("name", s.name.map(l => JString(l)).orNull),
      JField("change_date", s.changeDate.map(l => JString(l)).orNull),
      JField("ely", s.ely.map(l => JInt(BigInt.apply(l))).orNull),
      JField("muutospvm", s.trModifiedDate.map(l => JString(l)).orNull),
      JField("user", s.user.map(l => JString(l)).orNull),
      JField("published_date", s.trPublishedDate.map(l => JString(l)).orNull),
      JField("job_number", s.trJobNumber.map(l => JInt(BigInt.apply(l))).orNull),
      JField("error_message", s.errorMessage.map(l => JString(l)).orNull),
      JField("start_time", s.trProcessingStarted.map(l => JString(l)).orNull),
      JField("end_time", s.trProcessingEnded.map(l => JString(l)).orNull),
      JField("error_code", s.errorCode.map(l => JInt(BigInt.apply(l))).orNull))
}))

case object ChangeInfoRoadPartsSerializer extends CustomSerializer[RoadAddressChangeRecipient](format => ( {
  case o: JObject =>
    def jIntToLong(jInt: Any): Long = {
      jInt.asInstanceOf[BigInt].longValue()
    }
    val map = o.values
    val (road, track, startPart, stm, endPart, enm) =
      (map.get("tie"), map.get("ajr"), map.get("aosa"), map.get("aet"), map.get("losa"), map.get("let"))
    RoadAddressChangeRecipient(road.map(jIntToLong), track.map(jIntToLong), startPart.map(jIntToLong), endPart.map(jIntToLong),
      stm.map(jIntToLong), enm.map(jIntToLong))
}, {
  case s: RoadAddressChangeRecipient =>
    JObject(JField("tie", s.roadNumber.map(l => JInt(BigInt.apply(l))).orNull),
      JField("ajr", s.trackCode.map(l => JInt(BigInt.apply(l))).orNull),
      JField("aosa", s.startRoadPartNumber.map(l => JInt(BigInt.apply(l))).orNull),
      JField("aet", s.startAddressM.map(l => JInt(BigInt.apply(l))).orNull),
      JField("losa", s.endRoadPartNumber.map(l => JInt(BigInt.apply(l))).orNull),
      JField("let", s.endAddressM.map(l => JInt(BigInt.apply(l))).orNull))
}))

object ViiteTierekisteriClient {

  lazy val properties: Properties = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("/digiroad2.properties"))
    props
  }

  private def getRestEndPoint: String = {
    val isTREnabled = properties.getProperty("digiroad2.tierekisteri.enabled") == "true"
    val loadedKeyString = if(isTREnabled){
      properties.getProperty("digiroad2.tierekisteriViiteRestApiEndPoint")
    }  else "http://localhost:8080/trrest/"
    println("viite-endpoint = "+loadedKeyString)
    if (loadedKeyString == null)
      throw new IllegalArgumentException("Missing TierekisteriViiteRestApiEndPoint")
    loadedKeyString
  }

  def sendRoadAddressChangeData(changeData: List[ProjectRoadAddressChange]) = {
    val projects = changeData.map(cd => {
      convertChangeDataToChangeProject(cd)
    })
    val grouped = projects.groupBy(p => (p.id, p.ely, p.name, p.changeDate, p.user))
    if (grouped.keySet.size > 1)
      throw new IllegalArgumentException("Multiple projects, elys, users or change dates in single data set")
    val project = projects.tail.foldLeft(projects.head) { case (proj1, proj2) =>
      proj1.copy(changeInfoSeq = proj1.changeInfoSeq ++ proj2.changeInfoSeq)
    }
    sendJsonMessage(project)
  }

  private def convertChangeDataToChangeProject(changeData: ProjectRoadAddressChange): ChangeProject = {
    val changeInfo = changeData.changeInfo
    ChangeProject(changeData.projectId, changeData.projectName.getOrElse(""), changeData.user, changeData.ely,
      DateTimeFormat.forPattern("yyyy-MM-DD").print(changeData.changeDate), Seq(changeInfo))
  }

  private val auth = new TierekisteriAuthPropertyReader

  private val client = HttpClientBuilder.create().build

  def createJsonmessage(trProject:ChangeProject) = {
    implicit val formats = DefaultFormats + ChangeInfoRoadPartsSerializer + ChangeInfoItemSerializer + ChangeProjectSerializer
    val json = Serialization.write(Extraction.decompose(trProject))
    new StringEntity(json, ContentType.APPLICATION_JSON)
  }

  def sendJsonMessage(trProject:ChangeProject): ProjectChangeStatus ={
    val request = new HttpPost(getRestEndPoint+"addresschange/")
    request.addHeader("X-OTH-Authorization", "Basic " + auth.getAuthInBase64)
    request.setEntity(createJsonmessage(trProject))
    val response = client.execute(request)
    val statusCode = response.getStatusLine.getStatusCode
    val reason = response.getStatusLine.getReasonPhrase
    ProjectChangeStatus(trProject.id, statusCode, reason)
  }

  def getProjectStatus(projectid:String): Map[String,Any] =
  {
    implicit val formats = DefaultFormats
    val request = new HttpGet(getRestEndPoint+"addresschange/"+projectid)
    request.addHeader("X-OTH-Authorization", "Basic " + auth.getAuthInBase64)
    val response = client.execute(request)
    val receivedData=parse(StreamInput(response.getEntity.getContent)).extract[TRProjectStatus]
    Map(
      "id"->receivedData.id,
      "id_tr_projekti"-> receivedData.trProjectId.getOrElse("null"),
      "projekti"-> receivedData.trSubProjectId.getOrElse("null"),
      "tunnus"->receivedData.trTrackingCode.getOrElse("null"),
      "status"->receivedData.status.getOrElse("null"),
      "name"-> receivedData.name.getOrElse("null"),
      "change_date"->receivedData.changeDate.getOrElse("null"),
      "ely"->receivedData.ely.getOrElse("null"),
      "muutospvm"->receivedData.trModifiedDate.getOrElse("null"),
      "user"->receivedData.user.getOrElse("null"),
      "published_date"->receivedData.trPublishedDate.getOrElse("null"),
      "job_number"->receivedData.trJobNumber.getOrElse("null"),
      "error_message"->receivedData.errorMessage.getOrElse("null"),
      "start_time"->receivedData.trProcessingStarted.getOrElse("null"),
      "end_time"->receivedData.trProcessingEnded.getOrElse("null"),
      "error_code"->receivedData.errorCode.getOrElse("null")
    )

  }

  def getProjectStatusObject(projectid:Long): Option[TRProjectStatus] = {

    implicit val formats = DefaultFormats
    val request = new HttpGet(s"${getRestEndPoint}addresschange/$projectid")
    request.addHeader("X-OTH-Authorization", "Basic " + auth.getAuthInBase64)

    val response = client.execute(request)
    try {
      val  receivedData = parse(StreamInput(response.getEntity.getContent)).extract[TRProjectStatus]
      response.close()
      return Option(receivedData)
    } catch {
      case NonFatal(e) => None
    }finally {
      response.close()
    }
  }
}