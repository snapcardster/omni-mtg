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
import javax.json.{Json, JsonStructure, JsonValue}
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

  val snapBaseUrl: String = "https://test.snapcardster.com"
  val snapCsvEndpoint: String = snapBaseUrl + "/importer/sellerdata/from/csv"
  val snapChangedEndpoint: String = snapBaseUrl + "/marketplace/sellerdata/changed"

  val mkmBaseUrl: String = "https://www.mkmapi.eu/ws/v2.0"
  val mkmStockEndpoint: String = mkmBaseUrl + "/stock"
  val mkmStockFileEndpoint: String = mkmBaseUrl + "/output.json/stock/file"

  def insertFromClip(mode: String): Unit = {
    val data = String.valueOf(Toolkit.getDefaultToolkit.getSystemClipboard.getData(DataFlavor.stringFlavor))
    mode match {
      case "mkm" =>
        val p: Pattern = Pattern.compile(".*App token\\s*(.*)\\s+App secret\\s*(.*)\\s+Access token\\s*(.*)\\s+Access token secret\\s*(.*)\\s*.*?")
        val matcher: Matcher = p.matcher(data)
        if (matcher.find) {
          mkmAppToken.setValue(matcher.group(1))
          mkmAppSecret.setValue(matcher.group(2))
          mkmAccessToken.setValue(matcher.group(3))
          mkmAccessTokenSecret.setValue(matcher.group(4))
        }
      case "snap" =>
        val p: Pattern = Pattern.compile(".*User\\s*(.*)\\s+Token\\s*(.*)\\s*.*?")
        val matcher: Matcher = p.matcher(data)
        if (matcher.find) {
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

  def newProp(name: String, value: String): SimpleStringProperty = {
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
            loadSnapChangedAndDeleteFromStock()

            val csv = loadMkmStock()
            output.setValue(new Date() + "\n" + csv.split("\n").length + " lines read from mkm stock")

            val res: String = postToSnap(csv)
            output.setValue(new Date() + "\n" + snapCsvEndpoint + "\n" + res)
          } catch {
            case e: Exception => handleEx(e)
          }

          val min = interval.getValue.intValue
          val seconds = min * 60
          for (_ <- 1.to(seconds)) {
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

  def loadSnapChangedAndDeleteFromStock(): Unit = {
    val json: String = loadChangedFromSnap()

    output.setValue(new Date() + "\n" + snapChangedEndpoint + "\n" + json)
    /* val script =
       """
          var fun = function(json) {
            var res = [];
            var arr = JSON.parse(json);
            for(var index in arr) {
              var item = arr[index];
              res.push(item.type + "," + item.externalId);
            }
            return res.join("\n");
          }
       """
     val value = process(script, json).toString
     val list = value.split("\n").map { line => line.split(",") }.toList
     val longs = list.flatMap { parts =>
       if (parts(0) == "removed" || parts(0) == "reserved") {
         List(parts(1).toLong)
       } else {
         Nil
       }
     }*/
    val structure = fromJson(json)
    val list = structure.asJsonArray.toArray(new Array[JsonValue](0)).map { x =>
      val obj = x.asJsonObject
      val typeValue = obj.getString("type")
      val externalId = obj.getJsonNumber("externalId").longValue
      val info = obj.get("info")
      if (info != null) {
        typeValue -> Some(CsvFormat.parse(info.asJsonObject))
      } else {
        typeValue -> Some(CsvFormat(
          0, "", None, foil = false, Condition("", "", 0, ""), Language("", "", 0),
          "", None, None, signed = false, altered = false, None, Some(externalId)
        ))
      }
    }.toList

    val removedOrReservedItems = list.flatMap { parts =>
      if (parts._1 == "removed" || parts._1 == "reserved") {
        List(parts._2.get.externalId.get)
      } else {
        Nil
      }
    }
    deleteFromMkmStock(removedOrReservedItems)

    val addedItems = list.flatMap { parts =>
      if (parts._1 == "added" || parts._1 == "changed") {
        List(parts._2.get)
      } else {
        Nil
      }
    }
    addToMkmStock(addedItems)
  }

  def handleEx(e: Throwable, obj: Any = null): Unit = {
    if (e != null) {
      e.printStackTrace()
      output.setValue(e.toString + "\n" + e.getStackTrace.take(4).mkString("\n") + "\n" + obj)
    } else {
      output.setValue("Error here: \n" + obj)
    }
  }

  def postToSnap(csv: String): String = {
    val writer = new StringWriter()
    val map = new util.HashMap[String, AnyRef]
    map.put("fileName", "mkmStock.csv")
    map.put("fileContent", csv)
    Json.createWriter(writer).write(Json.createObjectBuilder(map).build)

    val body = writer.toString
    val res = new SnapConnector().call(snapCsvEndpoint, "POST", getAuth, body)
    res
  }

  // https://stackoverflow.com/a/42106801/773842
  def process(script: String, input: String): AnyRef = {
    val engine: ScriptEngine = new ScriptEngineManager().getEngineByName("nashorn")
    engine.eval(script)
    val invocable: Invocable = engine.asInstanceOf[Invocable]
    val body = invocable.invokeFunction("fun", input)
    body
  }

  def loadChangedFromSnap(): String = {
    val res = new SnapConnector().call(snapChangedEndpoint, "GET", getAuth)
    res
  }

  def fromJson(str: String): JsonStructure = {
    Json.createReader(new StringReader(str)).read
  }

  def getAuth: String = {
    snapUser.getValue + "," + snapToken.getValue
  }

  def loadMkmStock(): String = {
    val mkm = getMkm
    if (mkm.request(mkmStockFileEndpoint)) {
      val obj = Json.createReader(new StringReader(mkm.responseContent)).readObject
      val result = obj.getString("stock")

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

  def getMkm: M11DedicatedApp = {
    val x = new M11DedicatedApp(mkmAppToken.getValue, mkmAppSecret.getValue, mkmAccessToken.getValue, mkmAccessTokenSecret.getValue)
    x.setDebug(true)
    x
  }

  def deleteFromMkmStock(ids: List[Long]): Unit = {
    if (ids.isEmpty)
      return

    val mkm = getMkm

    val body =
      s"""
      <?xml version="1.0" encoding="UTF-8"?>
      <request>
      ${
        ids.map(id =>
          s"""
            <article>
              <idArticle>$id</idArticle>
              <count>1</count>
            </article>
            """
        ).mkString("")
      }
      </request>
      """
    if (!mkm.request(mkmStockEndpoint, "DELETE", body, "application/xml")) {
      handleEx(mkm.lastError, ids)
    }
  }

  def addToMkmStock(ids: List[CsvFormat]): Unit = {
    if (ids.isEmpty)
      return

    val mkm = getMkm

    val body =
      s"""
      <?xml version="1.0" encoding="UTF-8"?>
      <request>
      ${
        ids.map(id =>
          s"""
             <article>
               <idProduct>${id.externalId}</idProduct>
               <idLanguage>${id.language.code}</idLanguage>
               <comments>Inserted through the API</comments>
               <count>${id.qty}</count>
               <price>${id.price.get}</price>
               <condition>${id.condition.shortString}</condition>
               <isFoil>${id.foil}</isFoil>
               <isSigned>${id.signed}</isSigned>
               <isPlayset>false</isPlayset>
             </article>
            """
        ).mkString("")
      }
      </request>
      """
    if (!mkm.request(mkmStockEndpoint, "POST", body, "application/xml")) {
      handleEx(mkm.lastError, ids)
    }
  }
}
