import java.net.{HttpURLConnection, SocketTimeoutException, URL}

import junit.framework.TestCase
import org.junit.Test
import org.junit.Assert._

// @RunWith(BlockJUnit4ClassRunner.getClass)
class Tests {

  @Test
  def test(): Unit = {
    val requestURL = "https://api.snapcardster.com/sqlLatency"
    val auth = ""
    val method = "GET"
    val body = null
    val timeoutMs = 1000

    val lastCode: Int = connectTest(requestURL, auth, method, body, timeoutMs)

    assertEquals(200, lastCode)
  }

  @Test
  def test2(): Unit = {
    val requestURL = "https://api.snapcardster.com/sqlLatency"
    val auth = ""
    val method = "GET"
    val body = null
    val timeoutMs = 1

    var done = false
    try {
      connectTest(requestURL, auth, method, body, timeoutMs)
      done = true
    } catch {
      case e: Throwable =>
        assertNotNull(e)
        assertEquals(e.getClass.getSimpleName, "SocketTimeoutException")
    }
    if (done) {
      fail("should not reach this point")
    }
  }

  @Test
  def test3(): Unit = {
    val requestURL = "https://api.snapcardster.com/update/newCardDetails"
    val auth = "Pre-Launch-Test,9dd98c19dd6d0a83ee58e5f0020679a62320d22f307bc39ea90cdbffb66386d0"
    val method = "GET"
    val body = null
    val timeoutMs = 3000

    var done = false
    try {
      connectTest(requestURL, auth, method, body, timeoutMs)
      done = true
    } catch {
      case e: Throwable =>
        assertNotNull(e)
        assertEquals(e.getClass.getSimpleName, "SocketTimeoutException")
        done = false
    }
    if (done) {
      fail("should not reach this point")
    }
  }

  @Test
  def test4(): Unit = {
    val requestURL = "https://api.snapcardster.com/update/newCardDetails"
    val auth = "Pre-Launch-Test,9dd98c19dd6d0a83ee58e5f0020679a62320d22f307bc39ea90cdbffb66386d0"
    val method = "GET"
    val body = null
    val timeoutMs = 13000

    val lastCode = connectTest(requestURL, auth, method, body, timeoutMs)
    assertEquals(200, lastCode)
  }

  def connectTest(requestURL: String, auth: String, method: String, body: String, timeoutMs: Int): Int = {
    val connection: HttpURLConnection = new URL(requestURL).openConnection.asInstanceOf[HttpURLConnection]
    if (auth != null) {
      connection.addRequestProperty("Authorization", auth)
    }
    if (body != null) {
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Accept", "application/json")
    }

    connection.setRequestMethod(method)

    connection.setUseCaches(false)
    connection.setDoInput(true)
    if (body != null) {
      connection.setDoOutput(true)
      val outputInBytes = body.getBytes("UTF-8")
      val os = connection.getOutputStream
      os.write(outputInBytes)
    }

    connection.setConnectTimeout(timeoutMs)
    connection.setReadTimeout(timeoutMs)

    connection.connect()

    val lastCode = connection.getResponseCode
    lastCode
  }
}
