package omnimtg

import omnimtg.Interfaces._
import java.io._
import java.net.{HttpURLConnection, URL}

class SnapConnector(func: NativeFunctionProvider) {
  def call(requestURL: String, method: String, auth: String = null, body: String = null): String = {
    func.println(
      auth + ", " + method + ": " + requestURL + ", " +
        (if (body == null) "no body" else "body is a string of length " + body.length)
    )
    val connection: HttpURLConnection = new URL(requestURL).openConnection.asInstanceOf[HttpURLConnection]
    if (auth != null) {
      connection.addRequestProperty("Authorization", auth)
      func.println("Auth:" + auth)
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
      val os = connection.getOutputStream
      os.write(outputInBytes)
    }
    connection.setConnectTimeout(60 * 60 * 1000)
    func.println("connect...")
    connection.connect

    val lastCode = connection.getResponseCode
    func.println(requestURL + " response code:" + lastCode)


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
  }
}