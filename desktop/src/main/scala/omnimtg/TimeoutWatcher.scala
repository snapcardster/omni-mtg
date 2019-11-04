package omnimtg

import java.util.concurrent.TimeUnit

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class TimeoutWatcher[T](timeout: Int, task: () => T) {
  var future: Future[T] = _

  var state: String = TimeoutWatcher.Unstarted
  var exception: Throwable = _

  def run(): Option[T] = {
    try {
      this.state = TimeoutWatcher.Running
      this.future = Future(task())
      val res = Some(Await.result(future, Duration(timeout, TimeUnit.MILLISECONDS)))
      this.state = TimeoutWatcher.Finished
      res
    } catch {
      case e: TimeoutException =>
        this.exception = e
        this.state = TimeoutWatcher.Timeouted
        None
      case e: Throwable =>
        this.exception = e
        this.state = TimeoutWatcher.Exception
        None
    }
  }
}

object TimeoutWatcher {
  val Unstarted = "Unstarted"
  val Running = "Running"
  val Finished = "Finished"
  val Timeouted = "Timeouted"
  val Exception = "Exception"
}
