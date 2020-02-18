package controllers

import java.text.SimpleDateFormat

import javax.inject._

import play.api.libs.json._
import play.api.mvc._
import omnimtg._
import omnimtg.Interfaces._
import scala.collection._
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}

case class FreeMem(
                    heapSize: Long,
                    heapMaxSize: Long,
                    heapFreeSize: Long
                  ) {
  override def toString: String = {
    Map(
      "heapSize" -> (heapSize / 1024.0 / 1024.0 + " MB"),
      "heapMaxSize" -> (heapMaxSize / 1024.0 / 1024.0 + " MB"),
      "heapFreeSize" -> (heapFreeSize / 1024.0 / 1024.0 + " MB")
    ).toString
  }
}

case class JsStatus(
                     timeToNextSyncInSec: Int,
                     running: Boolean,
                     inSync: Boolean,
                     snapCallsSoFar: Option[Int],
                     mkmCallsSoFar: Option[Int],
                     callInfo: Option[String],
                     space: Option[String],
                     title: Option[String]
                   )

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
  fun.println("Version: " + mc.title + "\n" + getMem)
  mc.startServer(null)

  var now: String = getNow()
  var map: mutable.TreeMap[String, String] = mutable.TreeMap()

  def getNow(): String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    sdf.format(new java.util.Date())
  }

  def getStatus: Action[AnyContent] = Action.async {
    val newNow = getNow()
    if (newNow != now) {

      val snap = mc.snapCallsSoFar.getValue()
      val mkm = mc.mkmCallsSoFar.getValue()
      map.put(now, "mage=" + snap + ", mkm=" + mkm)

      now = newNow
      mc.snapCallsSoFar.setValue(0)
      mc.mkmCallsSoFar.setValue(0)
    }

    Future.successful(Ok(Json.toJson(JsStatus(
      timeToNextSyncInSec = mc.nextSync.getValue,
      running = mc.running.getValue,
      inSync = mc.inSync.getValue,

      snapCallsSoFar = Some(mc.snapCallsSoFar.getValue),
      mkmCallsSoFar = Some(mc.mkmCallsSoFar.getValue),
      callInfo = Some(map.map(x => x._1 + ": " + x._2).mkString("\n")),
      space = Some(
        "FreeMem: " + getMem +
          ", availableProcessors: " + Runtime.getRuntime.availableProcessors),
      title = Some(mc.title)
    )))
    )
  }

  def getMem() = {
    // Get current size of heap in bytes// Get current size of heap in bytes
    val heapSize = Runtime.getRuntime.totalMemory

    // Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
    val heapMaxSize = Runtime.getRuntime.maxMemory

    // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
    val heapFreeSize = Runtime.getRuntime.freeMemory

    val mem = FreeMem(heapSize, heapMaxSize, heapFreeSize)
    mem
  }

  def getSettings: Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.toJson(JsSettings(
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

    Future.successful(Ok(Json.toJson(mc.getLogs)))
  }

  def parseJs[T](req: Request[AnyContent], rds: Reads[T])(f: T => Future[Result]): Future[Result] = {
    req.body.asJson match {
      case None =>
        Future.successful(BadRequest(Json.toJson(Seq("No json body, hasBody: " + req.hasBody))))
      case Some(js) =>
        js.validate[T](rds) match {
          case JsError(x) =>
            Future.successful(BadRequest(Json.toJson(Seq("Wrong json body: " + x))))
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

  def postExit(): Action[AnyContent] = Action.async {
    val retCode = 1337
    fun.println("Exit now with " + retCode)
    Thread.sleep(100)
    System.exit(retCode)
    Future.successful(Ok(Json.toJson(Seq(
      "Ok, you probably won't see this, call status and see it I'm still here"
    ))))
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

    mc.request.setValue(new Runnable {
      override def run(): Unit =
        System.exit(retCode)
    })
    mc.running.setValue(false)

    Future.successful(Ok(Json.toJson(Seq(
      "Ok, ",
      "Running: " + mc.running.getValue,
      "Time to exit is: " + whenTime
    ))))
  }
}
