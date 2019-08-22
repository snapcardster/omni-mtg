import app.TimeoutWatcherTestImpl
import org.junit.Assert._
import org.junit.Test

class TimeoutWatcherTest {

  def testCase(taskDuration: Int, timeout: Int, data: Int = 0, exception: Boolean = false): String = {
    val testData = 42
    val watcher = TimeoutWatcherTestImpl[Int](timeout, () => {
      if (exception) {
        Thread.sleep(taskDuration)
        println("throwing after " + taskDuration + "ms, timeout: " + timeout)
        throw new IllegalArgumentException("as requested")
      } else {
        Thread.sleep(taskDuration)
        println("returning data after " + taskDuration + "ms, timeout: " + timeout)
        data
      }
    })
    val res = watcher.run()
    if (exception) {
      assertFalse(res.isDefined)
    }
    if (res.isDefined) {
      assertEquals(data, res.get)
    }

    /*assertEquals(TimeoutWatcherTestImpl.Unstarted, watcher.state)
    val res = watcher.run()
    assertEquals(testData, res.get)
    assertEquals(TimeoutWatcherTestImpl.Finished, watcher.state)*/
    watcher.state
  }

  @Test
  def test(): Unit = {
    assertEquals(TimeoutWatcherTestImpl.Finished, testCase(taskDuration = 200, timeout = 220))
    assertEquals(TimeoutWatcherTestImpl.Timeouted, testCase(taskDuration = 200, timeout = 100))
    assertEquals(TimeoutWatcherTestImpl.Finished, testCase(taskDuration = 200, timeout = 300, data = 2))
    assertEquals(TimeoutWatcherTestImpl.Timeouted, testCase(taskDuration = 200, timeout = 100, data = 2))
    assertEquals(TimeoutWatcherTestImpl.Timeouted, testCase(taskDuration = 200, timeout = 100, data = 2, exception = true))
    assertEquals(TimeoutWatcherTestImpl.Exception, testCase(taskDuration = 200, timeout = 300, data = 2, exception = true))
  }
}
