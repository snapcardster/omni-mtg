package app

import java.util.concurrent.TimeUnit

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

// junit cannot find app sources :( Copy from src dir before test
case class TimeoutWatcherTestImpl[T](timeout: Int, task: () => T) {
  var future: Future[T] = _

  var state: String = TimeoutWatcherTestImpl.Unstarted
  var exception: Throwable = _

  def run(): Option[T] = {
    try {
      this.state = TimeoutWatcherTestImpl.Running
      this.future = Future(task())
      val res = Some(Await.result(future, Duration(timeout, TimeUnit.MILLISECONDS)))
      this.state = TimeoutWatcherTestImpl.Finished
      res
    } catch {
      case e: TimeoutException =>
        this.exception = e
        this.state = TimeoutWatcherTestImpl.Timeouted
        None
      case e: Throwable =>
        this.exception = e
        this.state = TimeoutWatcherTestImpl.Exception
        None
    }
  }
}

object TimeoutWatcherTestImpl {
  val Unstarted = "Unstarted"
  val Running = "Running"
  val Finished = "Finished"
  val Timeouted = "Timeouted"
  val Exception = "Exception"
}
