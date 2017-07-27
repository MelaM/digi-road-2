package fi.liikennevirasto.viite.process

import fi.liikennevirasto.digiroad2.{GeometryUtils, Point}
import fi.liikennevirasto.digiroad2.asset.SideCode
import fi.liikennevirasto.digiroad2.util.{RoadAddressException, Track}
import fi.liikennevirasto.viite.RoadType
import fi.liikennevirasto.viite.dao.{ProjectLink, _}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
  * Calculate the effective change between the project and the current road address data
  */
object ProjectDeltaCalculator {

  val logger = LoggerFactory.getLogger(getClass)
  val checker = new ContinuityChecker(null) // We don't need road link service here
  def delta(projectId: Long): Delta = {
    val projectOpt = ProjectDAO.getRoadAddressProjectById(projectId)
    if (projectOpt.isEmpty)
      throw new IllegalArgumentException("Project not found")
    val project = projectOpt.get
    val projectLinks = ProjectDAO.getProjectLinks(projectId).groupBy(l => RoadPart(l.roadNumber,l.roadPartNumber))
    val currentAddresses = projectLinks.keySet.map(r => r -> RoadAddressDAO.fetchByRoadPart(r.roadNumber, r.roadPartNumber, true)).toMap
    val terminations = findTerminations(projectLinks, currentAddresses)

    Delta(project.startDate, terminations.sortBy(t => (t.discontinuity.value, t.roadType.value)))
  }

  private def findTerminations(projectLinks: Map[RoadPart, Seq[ProjectLink]], currentAddresses: Map[RoadPart, Seq[RoadAddress]]) = {
    val terminations = projectLinks.map{case (part, pLinks) => part -> (pLinks, currentAddresses.getOrElse(part, Seq()))}.mapValues{ case (pll, ra) =>
      ra.filter(r => pll.exists(pl => pl.linkId == r.linkId && pl.status == LinkStatus.Terminated))
    }
    terminations.filterNot(t => t._2.isEmpty).values.foreach(validateTerminations)
    terminations.values.flatten.toSeq
  }

  private def validateTerminations(roadAddresses: Seq[RoadAddress]) = {
    if (roadAddresses.groupBy(ra => (ra.roadNumber, ra.roadPartNumber)).keySet.size != 1)
      throw new RoadAddressException("Multiple or no road parts present in one termination set")
    val missingSegments = checker.checkAddressesHaveNoGaps(roadAddresses)
    if (missingSegments.nonEmpty)
      throw new RoadAddressException(s"Termination has gaps in between: ${missingSegments.mkString("\n")}") //TODO: terminate only part of the road part later
  }

  // TODO: When other than terminations are handled we partition also project links: section should include road type
  def projectLinkPartition(projectLinks: Seq[ProjectLink]): Seq[RoadAddressSection] = ???

  def partition(roadAddresses: Seq[RoadAddress]): Seq[RoadAddressSection] = {
    def combineTwo(r1: RoadAddress, r2: RoadAddress): Seq[RoadAddress] = {
      if (r1.endAddrMValue == r2.startAddrMValue && r1.discontinuity == Discontinuity.Continuous)
        Seq(r1.copy(endAddrMValue = r2.endAddrMValue, discontinuity = r2.discontinuity))
      else
        Seq(r2, r1)
    }
    def combine(roadAddressSeq: Seq[RoadAddress], result: Seq[RoadAddress] = Seq()): Seq[RoadAddress] = {
      if (roadAddressSeq.isEmpty)
        result.reverse
      else if (result.isEmpty)
        combine(roadAddressSeq.tail, Seq(roadAddressSeq.head))
      else
        combine(roadAddressSeq.tail, combineTwo(result.head, roadAddressSeq.head) ++ result.tail)
    }
    val grouped = roadAddresses.groupBy(ra => (ra.roadNumber, ra.roadPartNumber, ra.track))
    grouped.mapValues(v => combine(v.sortBy(_.startAddrMValue))).values.flatten.map(ra =>
      RoadAddressSection(ra.roadNumber, ra.roadPartNumber, ra.roadPartNumber,
        ra.track, ra.startAddrMValue, ra.endAddrMValue, ra.discontinuity, ra.roadType)
    ).toSeq
  }

  def isSplitLink(project1:ProjectLink, project2:ProjectLink, linksList:Seq[ProjectLink]): Boolean = {
    (GeometryUtils.areSame(project1.geom.head, project2.geom.head) || GeometryUtils.areSame(project1.geom.last, project2.geom.last)) && linksList.filter(l => l.linkId == project1.linkId).isEmpty
  }

  def trackCodeChangeInOtherLink(result: Seq[ProjectLink], firstLinkList: Seq[ProjectLink], secondLinkList: Seq[ProjectLink]): Seq[ProjectLink] = {
    if (!firstLinkList.isEmpty && !secondLinkList.isEmpty) {
      firstLinkList.head match {
        case prj => {
          val r = secondLinkList.foldLeft(result) { (newList, secondPrj) => {
            isSplitLink(prj, secondPrj, newList)
            match {
              case true => newList:+ prj
              case _ => newList
            }
          }}
          return trackCodeChangeInOtherLink((result ++ r).distinct, firstLinkList.tail, secondLinkList.filterNot(op => r.exists(_.linkId == op.linkId)))
        }
      }
    }
    return result
  }

  def getRoadLinkBefore(combinedLinks: Seq[ProjectLink], links: Seq[ProjectLink]): Option[ProjectLink] = {
    combinedLinks.find(l => {
      GeometryUtils.areSame(links.head.geom.head, l.geom.last)
    })
  }

  def getRoadLinkAfter(combinedLinks: Seq[ProjectLink], links: Seq[ProjectLink]): Option[ProjectLink] = {
    combinedLinks.find(l => {
      GeometryUtils.areSame(links.last.geom.last, l.geom.head)
    })
  }

  def getLength(projectLink: Option[ProjectLink]): Double = {
    projectLink match {
      case Some(proj) => proj.geometryLength
      case None => 0
    }
  }

  def updateBeforeAfter(projBefore:Option[ProjectLink], projAfter: Option[ProjectLink], values:(Double,Double)): Unit = {

    projBefore match {
      case Some(p) => ProjectDAO.updateMValuesLinkId(p.copy(endAddrMValue = values._2.toLong))
      case None =>
    }
    projAfter match {
      case Some(p) => ProjectDAO.updateMValuesLinkId(p.copy(startAddrMValue = values._1.toLong))
      case None =>
    }
  }

  def trackCodeChangeLengths(linkList: Seq[ProjectLink], otherProjectLinks:Seq[ProjectLink]):Seq[ProjectLink] = {

    //if (!otherProjectLinks.isEmpty) {

      val newLinks = trackCodeChangeInOtherLink(Seq(), linkList,otherProjectLinks)  match {
        case list if !list.isEmpty => list
        case _ => return linkList
      }

      val existingLinksToChange = trackCodeChangeInOtherLink(Seq(), otherProjectLinks, linkList).filterNot(el => newLinks.exists(_.linkId == el.linkId))
      val combinedLinks = otherProjectLinks.filterNot(op => (existingLinksToChange ++ newLinks).exists(_.linkId == op.linkId))
      val roadLinkBefore = getRoadLinkBefore(combinedLinks, existingLinksToChange)
      val roadLinkAfter = getRoadLinkAfter(combinedLinks,existingLinksToChange)
      val newMValues = calculateLengths(newLinks, existingLinksToChange,getLength(roadLinkBefore),getLength(roadLinkAfter))

      updateBeforeAfter(roadLinkBefore,roadLinkAfter,newMValues)
      ProjectDAO.updateMValuesLinkId(existingLinksToChange.head.copy(startAddrMValue = newMValues._1.toLong))
      ProjectDAO.updateMValuesLinkId(existingLinksToChange.last.copy(endAddrMValue = newMValues._2.toLong))

      val t = newLinks.map(l => {
        if (l.linkId == newLinks.head.linkId) l.copy(startAddrMValue = newMValues._1.toLong)
        if (l.linkId == newLinks.last.linkId) l.copy(endAddrMValue = newMValues._2.toLong)
        else {
          l.copy(calibrationPoints = (None,None))
        }
      })
      t

    //} else {
    //  linkList
    //}
  }

  def getSplitStart(combinedLinks: Seq[ProjectLink], otherLinks: Seq[ProjectLink]): Double = {
    otherLinks.map(link => {
      combinedLinks.find(comb => {
        GeometryUtils.areAdjacent(comb.geom.last, link.geom.head)
      })
    }) match {
      case list if list.size > 0 => list.head.get.startMValue
      case _ => 0
    }
  }

  def calculateLengths(newLinks: Seq[ProjectLink], existingLinks:Seq[ProjectLink], beforeLength: Double, afterLength: Double): (Double,Double) = {
    val newLinkLength = newLinks.foldLeft(0.0){(sum,length) => {sum + length.geometryLength}}
    val existingLinkLength = existingLinks.foldLeft(0.0){(sum,length) => {sum + length.geometryLength}}
    (beforeLength,(newLinkLength + existingLinkLength)/2 + beforeLength)
  }

  def orderProjectLinksTopologyByGeometry(list:Seq[ProjectLink]): Seq[ProjectLink] = {

    def linkTopologyByGeomEndPoints(sortedList: Seq[ProjectLink], unprocessed: Seq[ProjectLink]) : Seq[ProjectLink] = {
      unprocessed.find(l => GeometryUtils.areAdjacent(l.geom, sortedList.last.geom)) match {
        case Some(prj) =>
          val changedPrj = invertSideCode(prj.geom, sortedList.last.geom) match {
            case true => prj.copy(sideCode = if(sortedList.last.sideCode == SideCode.TowardsDigitizing) SideCode.AgainstDigitizing else SideCode.TowardsDigitizing)
            case _ => prj.copy(sideCode = sortedList.last.sideCode)
          }
          val unp = unprocessed.filterNot(u => u.linkId == changedPrj.linkId)
          linkTopologyByGeomEndPoints(sortedList ++ Seq(changedPrj), unp)
        case _ => sortedList
      }
    }

    def invertSideCode(geom1 : Seq[Point], geom2 : Seq[Point]): Boolean = {
      GeometryUtils.areAdjacent(geom1.head, geom2.head) || GeometryUtils.areAdjacent(geom1.last, geom2.last)
    }


    val linksAtEnd = list.filterNot(e =>  linkAdjacentInBothEnds(e.linkId, e.geom, list.filterNot(l =>l.linkId ==e.linkId).map(_.geom)))
    //TODO which one of the possible ones (ones at the borders) should be the first? discover by sideCode? How?
    val rest = list.filterNot(_.linkId == linksAtEnd.head.linkId )
    val possibleFirst = rest.exists(p => GeometryUtils.areAdjacent(p.geom.head, linksAtEnd.head.geom.head) || GeometryUtils.areAdjacent(p.geom.last, linksAtEnd.head.geom.head)) match {
      case true => linksAtEnd.head.copy(sideCode = SideCode.AgainstDigitizing)
      case _ => linksAtEnd.head
    }
    linkTopologyByGeomEndPoints(Seq(possibleFirst), rest)
  }

  def linkAdjacentInBothEnds(linkId: Long, geometry1: Seq[Point], geometries: Seq[Seq[Point]]): Boolean = {
    val epsilon = 0.01
    val geometry1EndPoints = GeometryUtils.geometryEndpoints(geometry1)
    val isLeftAdjacent = geometries.exists { g =>
      val geometry2Endpoints = GeometryUtils.geometryEndpoints(g)
      geometry1EndPoints._1.distance2DTo(geometry2Endpoints._1) < epsilon ||
        geometry1EndPoints._1.distance2DTo(geometry2Endpoints._2) < epsilon
    }
    val isRightAdjacent = geometries.exists { g =>
      val geometry2Endpoints = GeometryUtils.geometryEndpoints(g)
      geometry1EndPoints._2.distance2DTo(geometry2Endpoints._1) < epsilon ||
        geometry1EndPoints._2.distance2DTo(geometry2Endpoints._2) < epsilon
    }
    isLeftAdjacent && isRightAdjacent
  }

  def determineMValues(projectLinks: Seq[ProjectLink], geometryLengthList: Map[RoadPartBasis, Seq[RoadPartLengths]], linksInProject: Seq [ProjectLink]): Seq[ProjectLink] = {
    val groupedProjectLinks = projectLinks.groupBy(record => (record.roadNumber, record.roadPartNumber))
    groupedProjectLinks.flatMap(gpl => {
      var lastEndM = 0.0
      val roadPartId = new RoadPartBasis(gpl._1._1, gpl._1._2)
      if(geometryLengthList.keySet.contains(roadPartId)){
        val links = orderProjectLinksTopologyByGeometry(gpl._2)
        val linksToCheck= links.map(l => {
          val lengths = geometryLengthList.get(roadPartId).get
          val foundGeomLength = lengths.find(_.linkId == l.linkId).get
          val endValue = lastEndM + foundGeomLength.geometryLength
          val updatedProjectLink = l.copy(startMValue = 0.0, endMValue = foundGeomLength.geometryLength, startAddrMValue = Math.round(lastEndM), endAddrMValue = Math.round(endValue))
          lastEndM = endValue
          updatedProjectLink
        })
        trackCodeChangeLengths(linksToCheck, linksInProject)
      } else {
        orderProjectLinksTopologyByGeometry(gpl._2)
      }
    }).toSeq
  }

  def orderProjectLinks(unorderedProjectLinks: Seq[ProjectLink]): Seq[ProjectLink] = {
    def extending(link: ProjectLink, ext: ProjectLink) = {
      link.roadNumber == ext.roadNumber && link.roadPartNumber == ext.roadPartNumber &&
        link.track == ext.track
    }
    def extendChainByGeometry(ordered: Seq[ProjectLink], unordered: Seq[ProjectLink], sideCode: SideCode): Seq[ProjectLink] = {
      // First link gets the assigned side code
      if (ordered.isEmpty)
        return extendChainByGeometry(Seq(unordered.head.copy(sideCode=sideCode)), unordered.tail, sideCode)
      if (unordered.isEmpty) {
        return ordered
      }
      // Find a road address link that continues from current last link
      unordered.find { ral =>
        (unordered++ordered).exists(
          un => un.linkId != ral.linkId && GeometryUtils.areAdjacent(un.geom, ral.geom)
        )
      } match {
        case Some(link) =>
          val sideCode = if (isSideCodeChange(link.geom, ordered.last.geom))
            switchSideCode(ordered.last.sideCode)
          else
            ordered.last.sideCode
          extendChainByGeometry(ordered ++ Seq(link.copy(sideCode=sideCode)), unordered.filterNot(link.equals), sideCode)
        case _ => throw new InvalidAddressDataException("Non-contiguous road target geometry")
      }
    }
    def extendChainByAddress(ordered: Seq[ProjectLink], unordered: Seq[ProjectLink]): Seq[ProjectLink] = {
      if (ordered.isEmpty)
        return extendChainByAddress(Seq(unordered.head), unordered.tail)
      if (unordered.isEmpty)
        return ordered
      val (next, rest) = unordered.partition(u => extending(ordered.last, u))
      if (next.nonEmpty)
        extendChainByAddress(ordered ++ next, rest)
      else {
        val (previous, rest) = unordered.partition(u => extending(u, ordered.head))
        if (previous.isEmpty)
          throw new IllegalArgumentException("Non-contiguous road addressing")
        else
          extendChainByAddress(previous ++ ordered, rest)
      }
    }

    val orderedByAddress =  extendChainByAddress(Seq(unorderedProjectLinks.head), unorderedProjectLinks.tail)
    val orderedByGeometry = extendChainByGeometry(Seq(), orderedByAddress, orderedByAddress.head.sideCode)
    orderedByGeometry
  }

  def isSideCodeChange(geom1: Seq[Point], geom2: Seq[Point]): Boolean = {
    GeometryUtils.areAdjacent(geom1.last, geom2.last) ||
      GeometryUtils.areAdjacent(geom1.head, geom2.head)
  }

  def switchSideCode(sideCode: SideCode): SideCode = {
    // Switch between against and towards 2 -> 3, 3 -> 2
    SideCode.apply(5-sideCode.value)
  }
}

case class Delta(startDate: DateTime, terminations: Seq[RoadAddress])
case class RoadPart(roadNumber: Long, roadPartNumber: Long)
case class RoadAddressSection(roadNumber: Long, roadPartNumberStart: Long, roadPartNumberEnd: Long, track: Track,
                              startMAddr: Long, endMAddr: Long, discontinuity: Discontinuity, roadType: RoadType) {
  def includes(ra: RoadAddress): Boolean = {
    // within the road number and parts included
    ra.roadNumber == roadNumber && ra.roadPartNumber >= roadPartNumberStart && ra.roadPartNumber <= roadPartNumberEnd &&
      // and on the same track
      ra.track == track &&
      // and not starting before this section start or after this section ends
      !(ra.startAddrMValue < startMAddr && ra.roadPartNumber == roadPartNumberStart ||
        ra.startAddrMValue > endMAddr && ra.roadPartNumber == roadPartNumberEnd) &&
      // and not ending after this section ends or before this section starts
      !(ra.endAddrMValue > endMAddr && ra.roadPartNumber == roadPartNumberEnd ||
        ra.endAddrMValue < startMAddr && ra.roadPartNumber == roadPartNumberStart)
  }
}
case class RoadPartBasis (roadNumber: Long, roadPartNumber: Long)
case class RoadPartLengths(linkId: Long, geometryLength: Double)