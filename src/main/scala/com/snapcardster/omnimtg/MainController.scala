package com.snapcardster.omnimtg

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
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.{Document, Node, NodeList}

import scala.collection.mutable.ListBuffer
import scala.util.Try

class MainController {
  val title = "Omni MTG Sync Tool, v2018-07-04"

  // TODO: change back to test after test
  // val snapBaseUrl: String = "https://api.snapcardster.com"
  val snapBaseUrl: String = "https://test.snapcardster.com"
  //val snapBaseUrl: String = "http://localhost:9000"

  val snapCsvEndpoint: String = snapBaseUrl + "/importer/sellerdata/from/csv"
  val snapLoginEndpoint: String = snapBaseUrl + "/auth"
  val snapChangedEndpoint: String = snapBaseUrl + "/marketplace/sellerdata/changed"

  val mkmBaseUrl: String = "https://www.mkmapi.eu/ws/v2.0"
  val mkmStockEndpoint: String = mkmBaseUrl + "/stock"
  val mkmStockFileEndpoint: String = mkmBaseUrl + "/output.json/stock/file"

  var thread: Thread = _
  val prop: Properties = new Properties
  val configPath: Path = Paths.get("secret.properties")
  val backupPath: Path = Paths.get("mkm_backup_" + System.currentTimeMillis() + ".csv")
  var aborted: BooleanProperty = new SimpleBooleanProperty(false)
  var running: BooleanProperty = new SimpleBooleanProperty(false)

  val mkmAppToken: StringProperty = newProp("mkmApp", "")
  val mkmAppSecret: StringProperty = newProp("mkmAppSecret", "")
  val mkmAccessToken: StringProperty = newProp("mkmAccessToken", "")
  val mkmAccessTokenSecret: StringProperty = newProp("mkmAccessTokenSecret", "")
  val snapUser: StringProperty = newProp("snapUser", "")
  val snapPassword: StringProperty = newProp("snapPassword", "")
  val snapToken: StringProperty = newProp("snapToken", "")

  val output: StringProperty = new SimpleStringProperty("Output appears here. Click Start Sync to start. This requires valid api data.")
  val interval: IntegerProperty = new SimpleIntegerProperty(180)
  val nextSync: IntegerProperty = new SimpleIntegerProperty(0)

  var backupFirst = true

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

  def readProperties(): Unit = {
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
  }

  def start(): Unit = {
    readProperties()

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

  def loginSnap(): Unit = {
    output.setValue(outputPrefix() + "Logging in to Snapcardster...")
    val body = s"""{\"userId\":\"${snapUser.getValue}\",\"password\":\"${snapPassword.getValue}\"}"""
    output.setValue(outputPrefix() + body)
    val res = new SnapConnector().call(snapLoginEndpoint, "POST", body = body)
    output.setValue(outputPrefix() + res)
    val json: JsonStructure = fromJson(res)
    snapToken.setValue(json.asJsonObject.getString("token"))
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
    val t = new Thread(() =>
      while (!aborted.getValue)
        if (running.getValue) {
          nextSync.set(0)
          try {
            sync
          } catch {
            case e: Exception => handleEx(e)
          }

          val seconds = interval.getValue.intValue
          for (x <- 1.to(seconds)) {
            if (seconds == interval.getValue.intValue) { // abort wait if changed during wait
              nextSync.set(seconds - x)
              Thread.sleep(1000)
            } else {
              Thread.sleep(10)
            }
          }
        } else {
          Thread.sleep(1000)
        }
    )
    t.start()
    t
  }

  def sync(): Unit = {
    if (backupFirst) {
      // Create a local backup of the MKM Stock
      output.setValue(outputPrefix() + "Saving Backup of MKM Stock before doing anything")
      val csv = loadMkmStock()

      val writer = new PrintWriter(backupPath.toFile)

      try {
        writer.write(csv)
      } catch {
        case e: Exception => handleEx(e)
      } finally {
        writer.close()
      }

      backupFirst = false
    }

    val sb = new StringBuilder
    val res1 = loadSnapChangedAndDeleteFromStock(sb)

    sb.append("Loading MKM stock...\n")
    val csv = loadMkmStock()
    sb.append("  " + csv.count(x => x == '\n') + " lines read from mkm stock\n")
    output.setValue(sb.toString)

    val res = postToSnap(csv)
    // output.setValue(outputPrefix() + snapCsvEndpoint + "\n" + res)
    val items = getChangeItems(res)

    val info =
      if (items.isEmpty)
        "  No changes were done on the server, everything up to date"
      else
        readableChanges(items)

    sb.append("• MKM to Snapcardster, changes at Snapcardster:\n" + info)
    output.setValue(sb.toString)
  }

  private def readableChanges(items: Seq[SellerDataChanged]): String = {
    items.map(readableChangeEntry)
      .sortBy(x => x)
      .groupBy(x => x).toList.map(x => "  " + x._2.length + " " + x._1)
      .mkString("\n")
  }

  def readableChangeEntry(x: SellerDataChanged): String = {
    val csv = x.info.get
    x.`type` + " " + csv.name +
      " (" + csv.editionCode.get + ") " + csv.language.shortString + " " +
      csv.condition.shortString + (if (csv.foil) " Foil" else "") +
      (if (csv.altered) " Altered" else "") +
      (if (csv.signed) " Signed" else "") +
      " " + csv.price.get + "€ (CSV: " + csv.meta + ")"
  }

  def outputPrefix(): String = {
    new Date() + "\n"
  }

  def asJsonArray(structure: JsonStructure): Array[JsonValue] = {
    structure.asJsonArray.toArray(new Array[JsonValue](0))
  }

  def loadSnapChangedAndDeleteFromStock(info: StringBuilder): StringBuilder = {
    info.append(outputPrefix() + "• Snapcardster to MKM, changes at MKM:\n")
    val json = loadChangedFromSnap()

    output.setValue(outputPrefix() + snapChangedEndpoint + "\n" + json)
    val list = getChangeItems(json)

    val removedOrReservedItems = list.flatMap { parts =>
      if (parts.`type` == "removed" || parts.`type` == "reserved") {
        List(parts)
      } else {
        Nil
      }
    }


    info.append(
      "Will remove " + removedOrReservedItems.length + " items...\n"
        + readableChanges(removedOrReservedItems) + "\n"
    )
    output.setValue(info.toString)

    val resDel = deleteFromMkmStock(removedOrReservedItems)
    info.append(resDel + "\n")
    output.setValue(info.toString)

    var body = if (resDel.isEmpty) "[]" else resDel
    var res = new SnapConnector().call(snapChangedEndpoint, "POST", getAuth, body)
    info.append("  " + res + "\n")
    output.setValue(info.toString)

    val addedItems = list.flatMap { parts =>
      if (parts.`type` == "added" || parts.`type` == "changed") {
        List(parts)
      } else {
        Nil
      }
    }
    info.append(
      "Will add " + addedItems.length + " items...\n"
        + readableChanges(addedItems) + "\n"
    )
    output.setValue(info.toString)

    val resAdd = addToMkmStock(addedItems)
    info.append("  " + resAdd + "\n")
    output.setValue(info.toString)

    info.append("Notify snapcardster...\n")
    output.setValue(info.toString)

    body = if (resAdd.isEmpty) "[]" else resAdd
    res = new SnapConnector().call(snapChangedEndpoint, "POST", getAuth, body)

    info.append("  " + res + "\n")
    output.setValue(info.toString)

    info
  }

  def getChangeItems(json: String): Array[SellerDataChanged] = {
    val structure = fromJson(json)
    val res = asJsonArray(structure).map { x =>
      val obj = x.asJsonObject
      val typeValue = obj.getString("type")
      val info = Try(CsvFormat.parse(obj.getJsonObject("info"))).toOption
      val collectionId = Try(obj.getJsonNumber("collectionId").longValue).toOption
      val externalId = Try(obj.getJsonNumber("externalId").longValue).toOption
      SellerDataChanged(
        typeValue,
        externalId,
        collectionId,
        info
      )
    }
    res
  }

  def handleEx(e: Throwable, obj: Any = null): Unit = {
    if (e != null) {
      e.printStackTrace()
      output.setValue(errorText(e) + "\n" + obj)
    } else {
      output.setValue("Error here: \n" + obj)
    }
  }

  def errorText(e: Throwable): String = {
    if (e != null) {
      val res = e.toString + "\n" + e.getStackTrace.take(4).mkString("\n")
      res.substring(0, Math.min(res.length, 1000))
    } else {
      "null"
    }
  }

  def postToSnap(csv: String): String = {
    val map = Map("fileName" -> "mkmStock.csv", "fileContent" -> csv)
    val body = jsonFromMap(map)

    val res = new SnapConnector().call(snapCsvEndpoint, "POST", getAuth, body)
    res
  }

  // https://stackoverflow.com/a/42106801/773842
  /* def process(script: String, input: String): AnyRef = {
    val engine: ScriptEngine = new ScriptEngineManager().getEngineByName("nashorn")
    engine.eval(script)
    val invocable: Invocable = engine.asInstanceOf[Invocable]
    val body = invocable.invokeFunction("fun", input)
    body
  } */

  def jsonFromMap(map: Map[String, Any]): String = {
    val writer = new StringWriter()
    val hashMap = new util.HashMap[String, AnyRef]
    for ((k, v) <- map) {
      hashMap.put(k, v.asInstanceOf[AnyRef])
    }
    Json.createWriter(writer).write(Json.createObjectBuilder(hashMap).build)
    writer.toString
  }

  def loadChangedFromSnap(): String = {
    val res = new SnapConnector().call(snapChangedEndpoint, "GET", getAuth)
    res
  }

  def fromJson(str: String): JsonStructure = {
    try {
      Json.createReader(new StringReader(str)).read
    } catch {
      case e: Exception =>
        sys.error("Error parsing JSON " + e + "\nJSON: " + str)
    }
  }

  def getAuth: String = {
    snapUser.getValue + "," + snapToken.getValue
  }

  def loadMkmStock(): String = {
    val mkm = getMkm
    val hasOutput = true
    if (mkm.request(mkmStockFileEndpoint, "GET", null, null, hasOutput)) {
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

  def deleteFromMkmStock(entries: Seq[SellerDataChanged]): String = {
    if (entries.isEmpty)
      return ""

    val mkm = getMkm

    val body = // NO SPACE at beginning, this is important
      s"""<?xml version="1.0" encoding="utf-8"?>
      <request>
      ${
        entries.map(entry =>
          s"""
            <article>
              <idArticle>${entry.externalId.get}</idArticle>
              <count>1</count>
            </article>
            """
        ).mkString("")
      }
      </request>
      """
    val hasOutput = false
    if (!mkm.request(mkmStockEndpoint, "DELETE", body, "application/xml", hasOutput)) {
      handleEx(mkm.lastError, entries)
      getErrorString(mkm)
    } else {
      getConfirmation(mkm.responseContent, "deleted", entries, added = false)
    }
  }

  def addToMkmStock(entries: Seq[SellerDataChanged]): String = {
    if (entries.isEmpty)
      return ""

    val mkm = getMkm

    val body = // NO SPACE at beginning, this is important
      s"""<?xml version="1.0" encoding="utf-8"?>
      <request>
      ${
        entries.map { entry =>
          // only insert ist supported right now, not update
          //val extIdInfo = ""
          // id.externalId match {
          //  case None => sys.error("cannot add to mkm, only update")
          //  case Some(x) => "<idArticle>" + x + "</idArticle>"
          // }

          // Cannot change qty, see:
          // https://www.mkmapi.eu/ws/documentation/API_2.0:Stock
          val info = entry.info.get
          val csvLine = info.meta.replace("\"", "").split(";")
          // TODO: Apache CSV?
          val prodId = csvLine(1)
          val engName = csvLine(2)
          val localName = csvLine(3)
          val exp = csvLine(4)
          val expName = csvLine(5)
          val price = csvLine(6)
          val language = csvLine(7)
          val condition = csvLine(8)
          val foil = isTrue(csvLine(9))
          val signed = isTrue(csvLine(10))
          val playset = isTrue(csvLine(11))
          val altered = isTrue(csvLine(12))
          val comment = csvLine(13)
          val amount = csvLine(14)
          val onSale = csvLine(15)

          // Example:
          // "idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale"
          // "353185336";"294560";"Quicksmith Rebel";"Quicksmith Rebel";"AER";"Aether Revolt";"333.00";"1";"NM";"";"";"";"";"";"1";"1"

          // For post, all information must be present as well as count=1
          s"""
             <article>
               <idProduct>$prodId</idProduct>
               <condition>$condition</condition>
               <isFoil>$foil</isFoil>
               <isSigned>$signed</isSigned>
               <isPlayset>false</isPlayset>
               <idLanguage>$language</idLanguage>
               <isAltered>$altered</isAltered>
               <price>$price</price>
               <comments>$comment</comments>
               <count>1</count>
             </article>
            """
        }.mkString("")
      }
      </request>
      """
    val hasOutput = false
    if (!mkm.request(mkmStockEndpoint, "POST", body, "application/xml", hasOutput)) {
      handleEx(mkm.lastError, entries)
      getErrorString(mkm)
    } else {
      getConfirmation(mkm.responseContent, "inserted", entries, added = true)
    }
  }

  def isTrue(x: String): Boolean = {
    x.toLowerCase.contains("x") || x == "1" || x.toLowerCase == "true"
  }

  def xmlList(list: NodeList): List[Node] = {
    0.until(list.getLength).map(x => list.item(x)).toList
  }

  /**
    * https://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
    * https://www.owasp.org/index.php/Injection_Prevention_Cheat_Sheet_in_Java#XML:_External_Entity_attack
    */
  def getXml(xmlDoc: String): Document = {
    val dbFactory = DocumentBuilderFactory.newInstance
    dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    dbFactory.setXIncludeAware(false)
    dbFactory.setExpandEntityReferences(false)
    val dBuilder = dbFactory.newDocumentBuilder
    val doc = dBuilder.parse(new ByteArrayInputStream(xmlDoc.getBytes))
    doc.getDocumentElement.normalize()
    doc
  }

  def getErrorString(mkm: M11DedicatedApp): String = {
    s"""{"error": s"Returned HTTP code ${mkm.responseCode}, exception: ${errorText(mkm.lastError)}"}"""
  }

  def getConfirmation(xml: String, tagName: String, entries: Seq[SellerDataChanged], added: Boolean): String = {
    val lst = xmlList(getXml(xml).getElementsByTagName(tagName))
    val buf = new ListBuffer[SellerDataChanged]
    buf.append(entries: _*)

    val items: Seq[Map[String, Any]] = lst.flatMap { x =>
      val xs = xmlList(x.getChildNodes)
      val success = java.lang.Boolean.parseBoolean(xs.find(_.getNodeName == "success").get.getTextContent)
      val message = xs.find(_.getNodeName == "message").map(_.getTextContent)
      val value = xs.find(_.getNodeName == "idArticle").get
      // val englishName = xs.find(_.getNodeName == "engName").map(_.getTextContent)

      val ch = xmlList(value.getChildNodes)
      val idArticle =
        if (ch.size == 1 && ch.head.getNodeType == Node.TEXT_NODE) {
          ch.head.getTextContent.toLong
        } else {
          ch.find(_.getNodeName == "idArticle").get.getTextContent.toLong
        }

      val index = buf.indexWhere(b => b.externalId.get == idArticle)

      val collId =
        if (index == -1) {
          // sys.error("This should not occur: The element with id " + idArticle + " is not fund in mkm xml")
          -1
        } else {
          val item = buf(index)
          // We remove the element after finding it to prevent duplicate findings
          // (one idArticle maps to many collection ids)
          buf.remove(index)
          item.collectionId.get
        }

      val mapEntry =
        if (collId == -1) {
          None
        } else {
          if (message.isDefined) {
            Some(Map(
              "info" -> message.get,
              "collectionId" -> collId,
              "successful" -> success,
              "added" -> added
            ))
          } else {
            Some(Map(
              "collectionId" -> collId,
              "successful" -> success,
              "added" -> added
            ))
          }
        }
      mapEntry
    }

    // If there are entries left without a matching item, they got a new articleId and must be removed and (in the next sync) new inserted to snapcardster
    val needToRemove: Seq[Map[String, Any]] = (if (added) entries.filter(e => !items.exists(i => i.getOrElse("collectionId", 0).toString == e.collectionId.getOrElse(-1).toString)) else Nil).map { x =>
      Map("collectionId" -> x.collectionId.getOrElse(-1),
        "successful" -> false,
        "added" -> added,
        "info" -> "needToRemoveFromSnapcardster")
    }

    val body = "[" + (items ++ needToRemove).map(a => jsonFromMap(a)).mkString(", ") + "]"
    body
  }
}
