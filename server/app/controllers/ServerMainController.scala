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

case class JsSettings(
                       enabled: Boolean,
                       intervalInSec: Int,

                       bidPriceMultiplier: Double,

                       maxBidPrice: Double,
                       minBidPrice: Double,
                       askPriceMultiplier: Double,

                       bidLanguages: List[Int],
                       bidConditions: List[Int],
                       bidFoils: List[Boolean]
                     )

object JsSettings {
  implicit val r: Reads[JsSettings] = Json.reads[JsSettings]
  implicit val w: Writes[JsSettings] = Json.writes[JsSettings]
}

class ServerFunctionProvider() extends omnimtg.DesktopFunctionProvider() {
  // val log = Logger("omnimtg")

  override def println(x: Any): Unit = {
    Logger.info(String.valueOf(x))
    // Predef.println(x)
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
      timeToNextSyncInSec = mc.nextSync.getValue,
      running = mc.running.getValue,
      inSync = mc.inSync.getValue
    ))))
  }

  def getSettings: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(JsSettings(
      enabled = mc.running.getValue,
      intervalInSec = mc.interval.getValue,
      bidPriceMultiplier = mc.bidPriceMultiplier.getValue,
      minBidPrice = mc.minBidPrice.getValue,
      maxBidPrice = mc.maxBidPrice.getValue,
      askPriceMultiplier = mc.askPriceMultiplier.getValue,

      bidLanguages = mc.bidLanguages,
      bidConditions = mc.bidConditions,
      bidFoils = mc.bidFoils
    ))))
  }

  def getLogs: Action[AnyContent] = Action.async {
    //object LogItem {
    //  implicit val r: Reads[LogItem] = Json.reads[LogItem]
    implicit val w: Writes[LogItem] = Json.writes[LogItem]
    //}

    Future(Ok(Json.toJson(mc.logs.getValue.asInstanceOf[List[LogItem]])))
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
      mc.interval.setValue(x.intervalInSec)
      val changeList: List[(String, Any, Any)] =
        List(
          ("bidPriceMultiplier", mc.bidPriceMultiplier.getValue, x.bidPriceMultiplier),
          ("askPriceMultiplier", mc.askPriceMultiplier.getValue, x.askPriceMultiplier),
          ("minBidPrice", mc.minBidPrice.getValue, x.minBidPrice),
          ("maxBidPrice", mc.maxBidPrice.getValue, x.maxBidPrice),
          ("bidLanguages", mc.bidLanguages, x.bidLanguages),
          ("bidConditions", mc.bidConditions, x.bidConditions),
          ("bidFoils", mc.bidFoils, x.bidFoils)
        ).filter(x => x._2 != x._3)

      mc.bidPriceMultiplier.setValue(x.bidPriceMultiplier)

      mc.minBidPrice.setValue(x.minBidPrice)
      mc.maxBidPrice.setValue(x.maxBidPrice)

      mc.askPriceMultiplier.setValue(x.askPriceMultiplier)

      mc.bidLanguages = x.bidLanguages
      mc.bidConditions = x.bidConditions
      mc.bidFoils = x.bidFoils

      val old = mc.running.getValue
      mc.running.setValue(x.enabled)
      val res =
        if (x.enabled && !old) {
          Seq("Interval set", "Sync started")
        } else if (!x.enabled && old) {
          List("Interval set", "Sync stopped (current run is unaffected)")
        } else {
          List("Interval set", "Was started already")
        }
      val attrChange = changeList.map(x => x._1 + " changed from " + x._2 + " to " + x._3)
      Future.successful(Ok(Json.toJson(res ++ attrChange)))
    }
  }

  def postExitRequest(): Action[AnyContent] = Action.async {
    val retCode = 42
    var when = "exit is scheduled"
    var whenTime = mc.nextSync.getValue
    if (!mc.inSync.getValue) {
      when = "exiting now"
      whenTime = 0
      new Thread(() => {
        Thread.sleep(1000)
        System.exit(retCode)
      }).start
    }

    mc.request.setValue(() => System.exit(retCode))
    mc.running.setValue(false)

    Future(Ok(Json.toJson(Seq(
      "Ok, ",
      "Running: " + mc.running.getValue,
      "Time to exit is: " + whenTime
    ))))
  }
}
