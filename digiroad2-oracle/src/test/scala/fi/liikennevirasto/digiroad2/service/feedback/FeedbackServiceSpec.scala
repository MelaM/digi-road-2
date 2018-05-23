package fi.liikennevirasto.digiroad2.service.feedback

import fi.liikennevirasto.digiroad2.util.TestTransactions
import org.scalatest.{FunSuite, Matchers}

class FeedbackServiceSpec extends FunSuite with Matchers {

  def runWithRollback(test: => Unit): Unit = TestTransactions.runWithRollback()(test)

  val service = new FeedbackService(){
    override def withDynTransaction[T](f: => T): T = f
    override def withDynSession[T](f: => T): T = f
  }


  test("Create new feedback") {
    runWithRollback {
      val id = service.insertApplicationFeedback(Some("feedback_receiver"), Some("feedback_createdBy"), Some("Feedback body..."), Some("Feedback-Subject"), status = false, None)
      val feedbacks = service.getApplicationFeedbacksByIds(Set(id)).head
      feedbacks.body should be (Some("Feedback body..."))
      feedbacks.subject should be (Some("Feedback-Subject"))
      feedbacks.id should be (id)
    }
  }


  test("Get all not sent feedbacks") {
    runWithRollback {
      val (id, id1, id2) =(
        service.insertApplicationFeedback(Some("feedback_receiver"), Some("feedback_createdBy"), Some("Feedback body..."), Some("Feedback-Subject"), status = false, None),
        service.insertApplicationFeedback(Some("feedback_receiver1"), Some("feedback_createdBy1"), Some("Feedback body 1..."), Some("Feedback-Subject1"), status = false, None),
        service.insertApplicationFeedback(Some("feedback_receiver2"), Some("feedback_createdBy2"), Some("Feedback body 2..."), Some("Feedback-Subject2"), status = true, None))

      val feedbacks = service.getNotSentFeedbacks
      feedbacks.length should be (2)
      feedbacks.map(_.id) should contain (id)
      feedbacks.map(_.id) should contain (id1)
    }
  }


  test("Get all feedbacks") {
    runWithRollback {
      val (id, id1, id2) =(
        service.insertApplicationFeedback(Some("feedback_receiver"), Some("feedback_createdBy"), Some("Feedback body..."), Some("Feedback-Subject"), status = false, None),
        service.insertApplicationFeedback(Some("feedback_receiver1"), Some("feedback_createdBy1"), Some("Feedback body 1..."), Some("Feedback-Subject1"), status = false, None),
        service.insertApplicationFeedback(Some("feedback_receiver2"), Some("feedback_createdBy2"), Some("Feedback body 2..."), Some("Feedback-Subject2"), status = true, None))

      val feedbacks = service.getAllApplicationFeedbacks
      feedbacks.length should be (3)
      feedbacks.map(_.id) should contain (id)
      feedbacks.map(_.id) should contain (id1)
      feedbacks.map(_.id) should contain (id2)

    }
  }


  test("Update feedback status") {
    runWithRollback {
      val (id, id1, id2) =(
        service.insertApplicationFeedback(Some("feedback_receiver"), Some("feedback_createdBy"), Some("Feedback body..."), Some("Feedback-Subject"), status = false, None),
        service.insertApplicationFeedback(Some("feedback_receiver1"), Some("feedback_createdBy1"), Some("Feedback body 1..."), Some("Feedback-Subject1"), status = false, None),
        service.insertApplicationFeedback(Some("feedback_receiver2"), Some("feedback_createdBy2"), Some("Feedback body 2..."), Some("Feedback-Subject2"), status = true, None))

      service.updateApplicationFeedbackStatus(id1)
      val feedbacks = service.getNotSentFeedbacks
      feedbacks.length should be (1)
      feedbacks.map(_.id) should contain (id)
    }
  }

}
