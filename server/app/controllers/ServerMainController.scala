package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import omnimtg._
import omnimtg.Interfaces._
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

case class JsStatus(timeToNextSyncInSec: Int, running: Boolean)

object JsStatus {
  implicit val r: Reads[JsStatus] = Json.reads[JsStatus]
  implicit val w: Writes[JsStatus] = Json.writes[JsStatus]
}

case class JsLog(timestamp: Long, text: String, deleted: List[String] = Nil, changed: List[String] = Nil, added: List[String] = Nil)

object JsLog {
  implicit val r: Reads[JsLog] = Json.reads[JsLog]
  implicit val w: Writes[JsLog] = Json.writes[JsLog]
}

case class JsSettings(enabled: Boolean, intervalInSec: Int)

object JsSettings {
  implicit val r: Reads[JsSettings] = Json.reads[JsSettings]
  implicit val w: Writes[JsSettings] = Json.writes[JsSettings]
}

class ServerFunctionProvider() extends omnimtg.DesktopFunctionProvider() {
  val log = Logger("omni")

  override def println(x: Any): Unit = {
    log.info(String.valueOf(x))
    Predef.println(x)
  }
}

@Singleton
class ServerMainController @Inject()(cc: ControllerComponents, implicit val executionContext: ExecutionContext) extends AbstractController(cc) {

  val fun = new ServerFunctionProvider
  val mc: MainController = new MainController(JavaFXPropertyFactory, fun)
  fun.println(mc.title)
  mc.startServer(null)

  def getStatus: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(JsStatus(timeToNextSyncInSec = mc.getNextSync.getValue, running = mc.getRunning.getValue))))
  }

  def getSettings: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(JsSettings(enabled = mc.getRunning.getValue, intervalInSec = mc.getInterval.getValue))))
  }

  def getLogs: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(Seq(JsLog(System.currentTimeMillis, mc.getOutput.getValue)))))
  }

  def parseJs[T](req: Request[AnyContent], rds: Reads[T])(f: T => Future[Result]): Future[Result] = {
    req.body.asJson match {
      case None =>
        Future(BadRequest(Json.toJson(Seq("No json body, hasBody: " + req.hasBody))))
      case Some(js) =>
        js.validate[T](rds) match {
          case JsError(x) =>
            Future(BadRequest(Json.toJson(Seq("Wrong json body: " + x))))
          case JsSuccess(x, _) =>
            f(x)
        }
    }
  }

  def postSettings: Action[AnyContent] = Action.async { req =>
    parseJs(req, JsSettings.r) { x: JsSettings =>
      mc.getInterval.setValue(x.intervalInSec)
      val old = mc.getRunning.getValue
      mc.getRunning.setValue(x.enabled)
      if (x.enabled && !old) {
        Future(Ok(Json.toJson(Seq("Interval set", "Sync started"))))
      } else if (!x.enabled && old) {
        Future(Ok(Json.toJson(Seq("Interval set", "Sync stopped (current run is unaffected)"))))
      } else {
        Future(Ok(Json.toJson(Seq("Interval set", "Was started already"))))
      }
    }
  }

  def postExitRequest(): Action[AnyContent] = Action.async {
    if (!mc.getRunning.getValue && !mc.getInSync.getValue) {
      System.exit(42)
    }

    mc.getRequest.setValue(() => System.exit(42))
    mc.getRunning.setValue(false)

    Future(Ok(Json.toJson(Seq(
      "Ok, exit is scheduled",
      "Running: " + mc.getRunning.getValue,
      "Time to exit is: " + mc.getNextSync.getValue
    ))))
  }
}
