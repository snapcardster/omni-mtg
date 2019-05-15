package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._

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

@Singleton
class ServerMainController @Inject()(cc: ControllerComponents, implicit val executionContext: ExecutionContext) extends AbstractController(cc) {

  def getStatus: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(Seq(JsStatus(4, running = true)))))
  }

  def getSettings: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(Seq("Yay"))))
  }

  def getLogs: Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(Seq(JsLog(System.currentTimeMillis, "test")))))
  }

  def parseJs[T, Res](req: Request[AnyContent], rds: Reads[T])(f: T => Future[Result]): Future[Result] = {
    req.body.asJson match {
      case None =>
        Future(BadRequest(Json.toJson(Seq("No json body"))))
      case Some(js) =>
        js.validate[T](rds) match {
          case JsError(x) =>
            Future(BadRequest(Json.toJson(Seq("wrong json body: " + x))))
          case JsSuccess(x, _) =>
            f(x)
        }
    }
  }

  def postSettings: Action[AnyContent] = Action.async { req =>
    parseJs(req, JsSettings.r) { x =>
      Future(Ok(Json.toJson(Seq("Ok: " + x))))
    }
  }

  def postExitRequest(): Action[AnyContent] = Action.async {
    Future(Ok(Json.toJson(Seq("ok, scheduled"))))
  }
}
