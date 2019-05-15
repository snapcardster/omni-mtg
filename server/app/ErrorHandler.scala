import akka.util.ByteString
import javax.inject.Inject
import play.api.Logger
import play.api.http.HttpEntity
import play.api.mvc.{RequestHeader, ResponseHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class ErrorHandler @Inject()(implicit val executionContext: ExecutionContext) extends play.api.http.HttpErrorHandler {
  def handle(x: String, e: Throwable = null, statusCode: Int): String = {
    val res = "Error " + statusCode + ": " + x + (if (e != null) " (" + e.getClass.getSimpleName + ")" else "")
    Logger.error(res)
    res
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val str = handle(message, null, statusCode)
    Future(Result(ResponseHeader(statusCode), HttpEntity.Strict(ByteString(str), None)))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val statusCode = 500
    val message = exception.getMessage
    val str = handle(message, exception, statusCode)
    Future(Result(ResponseHeader(statusCode), HttpEntity.Strict(ByteString(str), None)))
  }
}
