package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import omnimtg._
import omnimtg.Interfaces._
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

case class JsStatus(timeToNextSyncInSec: Int, running: Boolean, inSync: Boolean)

object JsStatus {
  implicit val r: Reads[JsStatus] = Json.reads[JsStatus]
  implicit val w: Writes[JsStatus] = Json.writes[JsStatus]
}

case class JsSettings(enabled: Boolean, intervalInSec: Int)

object JsSettings {
  implicit val r: Reads[JsSettings] = Json.reads[JsSettings]
  implicit val w: Writes[JsSettings] = Json.writes[JsSettings]
}

class ServerFunctionProvider() extends omnimtg.DesktopFunctionProvider() {
  // val log = Logger("omnimtg")

  override def println(x: Any): Unit = {
    Logger.info(String.valueOf(x))
    Predef.println(x)
  }
}

@Singleton
class ServerMainController @Inject()(cc: ControllerComponents, implicit val executionContext: ExecutionContext) extends AbstractController(cc) {

  val fun = new ServerFunctionProvider
  fun.println("Starting Omni Mtg Controller...")
  val mc: MainController = new MainController(JavaFXPropertyFactory, fun)
  fun.println("Version: " + mc.title + "\n")
  mc.startServer(null)

  def getStatus: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(JsStatus(
      timeToNextSyncInSec = mc.getNextSync.getValue,
      running = mc.getRunning.getValue,
      inSync = mc.getInSync.getValue
    ))))
  }

  def getSettings: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(JsSettings(enabled = mc.getRunning.getValue, intervalInSec = mc.getInterval.getValue))))
  }

  def getLogs: Action[AnyContent] = Action.async {
    //object LogItem {
    //  implicit val r: Reads[LogItem] = Json.reads[LogItem]
    implicit val w: Writes[LogItem] = Json.writes[LogItem]
    //}

    Future(Ok(Json.toJson(mc.getLogs.asInstanceOf[List[LogItem]])))
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
    val retCode = 42
    var when = "exit is scheduled"
    var whenTime = mc.getNextSync.getValue
    if (!mc.getInSync.getValue) {
      when = "exiting now"
      whenTime = 0
      new Thread(() => {
        Thread.sleep(1000)
        System.exit(retCode)
      }).start
    }

    mc.getRequest.setValue(() => System.exit(retCode))
    mc.getRunning.setValue(false)

    Future(Ok(Json.toJson(Seq(
      "Ok, ",
      "Running: " + mc.getRunning.getValue,
      "Time to exit is: " + whenTime
    ))))
  }
}
