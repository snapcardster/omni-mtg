package omnimtg

import java.io._
import java.net.{HttpURLConnection, URL}

class SnapConnector {
  def call(requestURL: String, method: String, auth: String, body: Object = null): String = {
    val connection: HttpURLConnection = new URL(requestURL).openConnection.asInstanceOf[HttpURLConnection]
    connection.addRequestProperty("Authorization", auth)
    if (body != null) {
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Accept", "application/json")
    }
    connection.setRequestMethod(method)
    connection.setUseCaches(false)
    connection.setDoInput(true)
    connection.setDoOutput(true)
    connection.connect
    if (body != null) {
      val str = body.toString
      val outputInBytes = str.getBytes("UTF-8")
      val os = connection.getOutputStream
      os.write(outputInBytes)
    }

    val lastCode = connection.getResponseCode

    if (200 == lastCode || 401 == lastCode || 404 == lastCode) {
      val str =
        if (lastCode == 200)
          connection.getInputStream
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
    } else {
      sys.error("Error: " + lastCode)
    }
  }
}
