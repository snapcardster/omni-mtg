package omnimtg

import omnimtg.Interfaces._
import java.io._
import java.net.{HttpURLConnection, URL}

class SnapConnector(func: NativeFunctionProvider) {
  def call(mc: MainController, requestURL: String, method: String, auth: String = null, body: String = null): String = {

    mc.snapCallsSoFar.setValue(mc.snapCallsSoFar.getValue + 1)

    val timeoutMs = Config.getTimeout

    val timeout = TimeoutWatcher(timeoutMs, () =>
      callCore(requestURL, method, auth, body)
    )
    timeout.run().getOrElse(
      sys.error("Timeout: " + method + " " + requestURL + " did not complete within " + timeoutMs + "ms\nInner: " + timeout.exception)
    )
  }

  def callCore(requestURL: String, method: String, auth: String = null, body: String = null): String = {
    func.println(
      method + ": " + requestURL + ", Auth: " + auth + ", " +
        (if (body == null) "no body" else "body is a string of length " + body.length)
    )
    val connection: HttpURLConnection = new URL(requestURL).openConnection.asInstanceOf[HttpURLConnection]

    try {
      if (auth != null) {
        connection.addRequestProperty("Authorization", auth)
        //func.println("Auth:" + auth)
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
        func.println(connection.getRequestMethod + "ing " + outputInBytes.length + " bytes...")
        if (Config.isVerbose) {
          func.println(connection.getRequestMethod + "ing <" + body + ">")
        }
        val os = connection.getOutputStream
        os.write(outputInBytes)
      }
      val timeoutMs = Config.getTimeout
      connection.setConnectTimeout(timeoutMs)
      connection.setReadTimeout(timeoutMs)
      func.println("connect to snapcardster, timeouts (conn/read) " + timeoutMs + " ms... @ " + new Date())
      connection.connect()

      val lastCode = connection.getResponseCode
      func.println(method + " " + requestURL + " response code:" + lastCode + " @ " + new Date())

      val str =
        if (lastCode == 200)
          connection.getInputStream
        else if (lastCode == 204)
          new ByteArrayInputStream("204 No Content".getBytes)
        else
          connection.getErrorStream
      if (str == null) {
        sys.error("Error: " + lastCode)
      }
      val rd = new BufferedReader(new InputStreamReader(str))
      val sb = new StringBuffer
      var aborted = false
      while (!aborted) {
        val line = rd.readLine
        if (line == null) {
          aborted = true
        } else {
          sb.append(line)
        }
      }
      sb.toString
    } finally {
      // https://stackoverflow.com/questions/4767553/safe-use-of-httpurlconnection
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
