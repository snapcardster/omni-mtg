package omnimtg

import java.awt.datatransfer._
import java.awt.{Desktop, Toolkit}
import java.io._
import java.net.URI
import java.nio.file._
import java.util
import java.util.regex._
import java.util.zip._
import java.util.{Base64, Date, Properties}

import javafx.beans.property._
import javafx.beans.value._
import javax.script._

class MainController {

  var thread: Thread = _
  val prop: Properties = new Properties
  val configPath: Path = Paths.get("secret.properties")
  var aborted: BooleanProperty = new SimpleBooleanProperty(false)
  var running: BooleanProperty = new SimpleBooleanProperty(false)

  val mkmAppToken: StringProperty = newProp("mkmApp", "Enter MKM App Token")
  val mkmAppSecret: StringProperty = newProp("mkmAppSecret", "Enter MKM App Secret")
  val mkmAccessToken: StringProperty = newProp("mkmAccessToken", "Enter MKM Access Token")
  val mkmAccessTokenSecret: StringProperty = newProp("mkmAccessTokenSecret", "Enter MKM Access Token Secret")
  val snapUser: StringProperty = newProp("snapUser", "Enter Snapcardster User Id")
  val snapToken: StringProperty = newProp("snapToken", "Enter Snapcardster Token")

  val output: StringProperty = new SimpleStringProperty("Output...")
  val interval: IntegerProperty = new SimpleIntegerProperty(3)

  val baseUrl = "https://test.snapcardster.com"
  val stockEndpoint = "/stock/file"
  val csvEndpoint = "/importer/sellerdata/from/csv"
  val changedEndpoint = "/marketplace/sellerdata/changed"

  def insertFromClip(mode: String): Unit = {
    val data = String.valueOf(Toolkit.getDefaultToolkit.getSystemClipboard.getData(DataFlavor.stringFlavor))
    mode match {
      case "mkm" =>
        val p: Pattern = Pattern.compile(".*App token\\s*(.*)\\s+App secret\\s*(.*)\\s+Access token\\s*(.*)\\s+Access token secret\\s*(.*)\\s*.*?")
        val matcher: Matcher = p.matcher(data)
        if (matcher.find()) {
          mkmAppToken.setValue(matcher.group(1))
          mkmAppSecret.setValue(matcher.group(2))
          mkmAccessToken.setValue(matcher.group(3))
          mkmAccessTokenSecret.setValue(matcher.group(4))
        }
      case "snap" =>
        val p: Pattern = Pattern.compile(".*User\\s*(.*)\\s+Token\\s*(.*)\\s*.*?")
        val matcher: Matcher = p.matcher(data)
        if (matcher.find()) {
          snapUser.setValue(matcher.group(1))
          snapToken.setValue(matcher.group(2))
        }
      case _ => ()
    }
  }

  def updateProp(str: String, property: StringProperty): Unit = {
    val value = prop.get(str)
    if (value != null && value != "") {
      property.setValue(String.valueOf(value))
    }
  }

  def start(): Unit = {
    if (!configPath.toFile.exists) {
      configPath.toFile.createNewFile
    }

    var str: InputStream = new ByteArrayInputStream(Array(42))
    try {
      str = new FileInputStream(configPath.toFile)
      prop.load(str)

      updateProp("mkmApp", mkmAppToken)
      updateProp("mkmAppSecret", mkmAppSecret)
      updateProp("mkmAccessToken", mkmAccessToken)
      updateProp("mkmAccessTokenSecret", mkmAccessTokenSecret)
      updateProp("snapUser", snapUser)
      updateProp("snapToken", snapToken)

      if (prop.get("mkmAppToken") != null) {
        output.setValue("Stored authentication information restored")
      }
    } catch {
      case e: Exception => handleEx(e)
    } finally {
      str.close()
    }

    thread = run()
  }

  private def newProp(name: String, value: String): SimpleStringProperty = {
    val stringProp = new SimpleStringProperty(value)
    val l = new ChangeListener[Any] {
      def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
        prop.put(name, String.valueOf(newValue))
      }
    }
    stringProp.addListener(l)
    stringProp
  }

  def save(): Unit = {
    var str: OutputStream = new ByteArrayOutputStream()
    try {
      str = new FileOutputStream(configPath.toFile)
      prop.store(str, null)
    } catch {
      case e: Exception => handleEx(e)
    } finally {
      str.close()
    }
  }

  def stop(): Unit = {
    thread.interrupt()
  }

  def openLink(url: String): Unit = {
    try {
      if (Desktop.isDesktopSupported) {
        val desktop: Desktop = Desktop.getDesktop
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
          desktop.browse(new URI(url))
        }
      }
    } catch {
      case e: Exception => handleEx(e)
    }
  }

  def run(): Thread = {
    val t = new Thread(() => {
      while (!aborted.getValue) try {
        if (running.getValue) {
          try {
            val json: String = loadChangedFromSnap()
            output.setValue(new Date() + "\n" + changedEndpoint + "\n" + json)

            /*val csv = loadMkmStock()
            output.setValue(new Date() + "\n" + csv.split("\n").length + " lines read from mkm stock")

            val res: String = postToSnap(csv)
            output.setValue(new Date() + "\n" + csvEndpoint + "\n" + res)*/
          } catch {
            case e: Exception => handleEx(e)
          }

          val min = interval.getValue.intValue
          val ms = min * 60 * 1000
          for (waitStep <- 1.to(ms / 1000)) {
            if (min == interval.getValue.intValue) {
              Thread.sleep(1000)
            } else {
              Thread.sleep(10)
            }
          }
        } else {
          Thread.sleep(1000)
        }
      } catch {
        case e: Exception => handleEx(e)
      }
    }
    )
    t.start()
    t
  }

  private def handleEx(e: Exception) = {
    output.setValue(e.toString + "\n" + e.getStackTrace.take(4).mkString("\n"))
  }

  private def postToSnap(csv: String): String = {
    val url = baseUrl + csvEndpoint

    val engine: ScriptEngine = new ScriptEngineManager().getEngineByName("nashorn")
    val script =
      """
      var fun = function(csv) {
        var obj = { fileName: "mkmStock.csv", fileContent: csv }
        return JSON.stringify(obj)
      }
      """
    engine.eval(script)
    val invocable: Invocable = engine.asInstanceOf[Invocable]
    val body = invocable.invokeFunction("fun", csv).toString

    val res = new SnapConnector().call(url, "POST", getAuth, body)
    res
  }

  def loadChangedFromSnap(): String = {
    val url = baseUrl + changedEndpoint
    val res = new SnapConnector().call(url, "GET", getAuth)
    res
  }

  def getAuth: String = {
    snapUser.getValue + "," + snapToken.getValue
  }

  def loadMkmStock(): String = {
    val mkm = new M11DedicatedApp(mkmAppToken.getValue, mkmAppSecret.getValue, mkmAccessToken.getValue, mkmAccessTokenSecret.getValue)
    if (mkm.request("https://www.mkmapi.eu/ws/v2.0/output.json" + stockEndpoint)) {

      // https://stackoverflow.com/a/42106801/773842
      val engine: ScriptEngine = new ScriptEngineManager().getEngineByName("nashorn")
      engine.eval("var fun = function(raw) {return JSON.parse(raw).stock}")
      val invocable: Invocable = engine.asInstanceOf[Invocable]
      val result: String = invocable.invokeFunction("fun", mkm.responseContent).toString

      // get string content from base64'd gzip
      val arr: Array[Byte] = Base64.getDecoder.decode(result)
      val asd: ByteArrayInputStream = new ByteArrayInputStream(arr)
      val gz: GZIPInputStream = new GZIPInputStream(asd)
      val rd: BufferedReader = new BufferedReader(new InputStreamReader(gz))
      val content: util.List[String] = new util.ArrayList[String]

      var abort = false
      while (!abort) {
        val line = rd.readLine
        if (line == null) {
          abort = true
        } else {
          content.add(line)
        }
      }
      val strArr = content.toArray(new Array[String](0))
      val csv = String.join("\n", strArr: _*)
      csv
    } else {
      var text = "Server response: " + mkm.responseCode + " "
      if (mkm.lastError != null) text += mkm.lastError.toString
      sys.error(text)
    }
  }

  def deleteFromMkmStock(ids: List[Long]): Unit = {
    val mkm = new M11DedicatedApp(mkmAppToken.getValue, mkmAppSecret.getValue, mkmAccessToken.getValue, mkmAccessTokenSecret.getValue)
    val articles = ids.map(x =>
      s"<article>\n<idArticle>${x}</idArticle>\n<count>1</count>\n</article>"
    ).mkString("\n")
    val body = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<request>\n" + articles + "\n</request>"
    if (mkm.request("https://www.mkmapi.eu/ws/v1.1/stock", "DELETE", body, "application/xml")) {
      // TODO
    }
  }
}
