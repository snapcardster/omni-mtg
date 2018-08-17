package com.snapcardster.omnimtg

import java.io._
import java.nio.file._
import java.util
import java.util.regex._
import java.util.zip._
import java.util.{Date, Properties}

import com.google.gson.{Gson, GsonBuilder}
import com.snapcardster.omnimtg.Interfaces._
import javax.xml.parsers.DocumentBuilderFactory
import org.apache.commons.lang.StringUtils
import org.w3c.dom.{Document, Node, NodeList}

import scala.collection.mutable.ListBuffer

class MainController(propFactory: PropertyFactory, nativeProvider: NativeFunctionProvider) extends MainControllerInterface {
  val title = "Omni MTG Sync Tool, v2018-08-17r"

  val version = "2"

  // TODO: change back to test after test
  val snapBaseUrl: String = "https://api.snapcardster.com"
  //val snapBaseUrl: String = "https://dev.snapcardster.com" //"https://test.snapcardster.com"
  //val snapBaseUrl: String = "http://localhost:9000"

  val snapCsvEndpoint: String = snapBaseUrl + s"/importer/sellerdata/from/csv/$version"
  val snapLoginEndpoint: String = snapBaseUrl + "/auth"
  val snapChangedEndpoint: String = snapBaseUrl + s"/marketplace/sellerdata/changed/$version"

  val mkmBaseUrl: String = "https://www.mkmapi.eu/ws/v2.0"
  val mkmStockEndpoint: String = mkmBaseUrl + "/stock"
  val mkmStockFileEndpoint: String = mkmBaseUrl + "/output.json/stock/file"

  private var thread: Thread = _
  private val prop: Properties = new Properties
  private val backupPath: Path = null
  //Paths.get("mkm_backup_" + System.currentTimeMillis() + ".csv")
  private val aborted: BooleanProperty = propFactory.newBooleanProperty(false)
  private val running: BooleanProperty = propFactory.newBooleanProperty(false)
  private val mkmAppToken: StringProperty = propFactory.newStringProperty("mkmApp", "", prop)
  private val mkmAppSecret: StringProperty = propFactory.newStringProperty("mkmAppSecret", "", prop)
  private val mkmAccessToken: StringProperty = propFactory.newStringProperty("mkmAccessToken", "", prop)
  private val mkmAccessTokenSecret: StringProperty = propFactory.newStringProperty("mkmAccessTokenSecret", "", prop)
  private val snapUser: StringProperty = propFactory.newStringProperty("snapUser", "", prop)
  private val snapPassword: StringProperty = propFactory.newStringProperty("snapPassword", "", prop)
  private val snapToken: StringProperty = propFactory.newStringProperty("snapToken", "", prop)
  private val output: StringProperty = propFactory.newStringProperty("Output appears here. Click Start Sync to start. This requires valid api data.")
  private val interval: IntegerProperty = propFactory.newIntegerProperty("interval", 180, prop)
  private val nextSync: IntegerProperty = propFactory.newIntegerProperty(0)

  var backupFirst = true

  def insertFromClip(mode: String, data: String): Unit = {
    mode match {
      case "mkm" =>
        val p: Pattern = Pattern.compile(".*App token\\s*(.*)\\s+App secret\\s*(.*)\\s+Access token\\s*(.*)\\s+Access token secret\\s*(.*)\\s*.*?")
        val matcher: Matcher = p.matcher(data)
        if (matcher.find) {
          mkmAppToken.setValue(matcher.group(1), true)
          mkmAppSecret.setValue(matcher.group(2), true)
          mkmAccessToken.setValue(matcher.group(3), true)
          mkmAccessTokenSecret.setValue(matcher.group(4), true)
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

  def readProperties(nativeBase: Object): Unit = {
    val ex = nativeProvider.readProperties(prop, this, nativeBase)
    if (ex != null) {
      handleEx(ex)
    }
  }

  def start(nativeBase: Object): Unit = {
    readProperties(nativeBase)
    running.setValue(true)

    thread = run(nativeBase)
  }

  def save(nativeBase: Object): Unit = {
    println("save props")
    val ex = nativeProvider.save(prop, nativeBase)
    if (ex != null) {
      handleEx(ex)
    }
  }

  def loginSnap(): Unit = {
    output.setValue(outputPrefix() + "Logging in to Snapcardster...")
    val body = s"""{\"userId\":\"${snapUser.getValue}\",\"password\":\"${snapPassword.getValue}\"}"""
    output.setValue(outputPrefix() + body)
    val res = new SnapConnector().call(snapLoginEndpoint, "POST", body = body)
    if (res == null) {
      snapToken.setValue("")
    } else {
      output.setValue(outputPrefix() + res)
      val json = new Gson().fromJson(res, classOf[Token])
      if (json == null) {
        snapToken.setValue("")
      } else {
        System.out.println("set token to " + json.token)
        snapToken.setValue(json.token)
      }
    }
  }

  def stop(): Unit = {
    running.setValue(false)
    thread.interrupt()
  }

  def openLink(url: String): Unit = {
    nativeProvider.openLink(url)
  }

  def run(nativeBase: Object): Thread = {
    val t = new Thread(new Runnable {
      override def run(): Unit = {
        while (!aborted.getValue) {
          if (running.getValue) {
            nextSync.setValue(0)
            try {
              sync(nativeBase)
            } catch {
              case e: Exception => handleEx(e)
            }

            val seconds = interval.getValue.intValue
            for (x <- 1.to(seconds)) {
              if (seconds == interval.getValue.intValue) { // abort wait if changed during wait
                nextSync.setValue(seconds - x)
                Thread.sleep(1000)
              } else {
                Thread.sleep(10)
              }
            }
          } else {
            Thread.sleep(1000)
          }
        }

      }
    })
    t.start()
    t
  }

  def sync(nativeBase: Object): Unit = {
    if (backupFirst) {
      // Create a local backup of the MKM Stock
      output.setValue(outputPrefix() + "Saving Backup of MKM Stock before doing anything")
      val csv = loadMkmStock()

      nativeProvider.saveToFile(s"backup_${System.currentTimeMillis()}.csv", csv, nativeBase)

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
    println("res:" + res)
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
    val csv = x.info
    x.`type` + " " + csv.name +
      " (" + csv.editionCode + ") " + csv.language.shortString + " " +
      csv.condition.shortString + (if (csv.foil) " Foil" else "") +
      (if (csv.altered) " Altered" else "") +
      (if (csv.signed) " Signed" else "") +
      " " + csv.price + "€ (CSV: " + csv.meta + ")"
  }

  def outputPrefix(): String = {
    new Date() + "\n"
  }

  def loadSnapChangedAndDeleteFromStock(info: StringBuilder): StringBuilder = {
    info.append(outputPrefix() + "• Snapcardster to MKM, changes at MKM:\n")
    val json = loadChangedFromSnap()

    println(snapChangedEndpoint + "\n" + json)

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
    try {
      new Gson().fromJson(json, classOf[Array[SellerDataChanged]])
    } catch {
      case x: Throwable =>
        sys.error(x.toString + "\n" + json)
    }
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
      val res = e.toString + "\n" + e.getStackTrace.mkString("\n")
      res.substring(0, Math.min(res.length, 8000))
    } else {
      "null"
    }
  }

  def postToSnap(csv: String): String = {
    val body = new Gson().toJson(MKMCsv("mkmStock.csv", csv))
    val res = new SnapConnector().call(snapCsvEndpoint, "POST", getAuth, body)
    res
  }

  def loadChangedFromSnap(): String = {
    val res = new SnapConnector().call(snapChangedEndpoint, "GET", getAuth, null)
    res
  }

  def getAuth: String = {
    snapUser.getValue + "," + snapToken.getValue
  }

  def loadMkmStock(): String = {
    val mkm = getMkm
    val hasOutput = true
    if (mkm.request(mkmStockFileEndpoint, "GET", null, null, hasOutput)) {

      val builder = new GsonBuilder
      val obj = new Gson().fromJson(mkm.responseContent(), classOf[MKMSomething])
      System.out.println(obj.toString)
      val result = obj.stock

      // get string content from base64'd gzip
      val arr: Array[Byte] = nativeProvider.decodeBase64(result)
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
      val csv = StringUtils.join(content, "\n")
      csv
    } else {
      var text = "Server response: " + mkm.responseCode + " "
      if (mkm.lastError != null) text += mkm.lastError.toString
      sys.error(text)
    }
  }

  def getMkm: M11DedicatedApp = {
    val x = new M11DedicatedApp(mkmAppToken.getValue, mkmAppSecret.getValue, mkmAccessToken.getValue, mkmAccessTokenSecret.getValue, nativeProvider)
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
              <idArticle>${entry.externalId}</idArticle>
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
          val info = entry.info
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
    //dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    //dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    //dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    //dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    //dbFactory.setXIncludeAware(false)
    dbFactory.setExpandEntityReferences(false)
    val dBuilder = dbFactory.newDocumentBuilder
    val doc = dBuilder.parse(new ByteArrayInputStream(xmlDoc.getBytes("UTF-8")))
    doc.getDocumentElement.normalize()
    doc
  }

  def getErrorString(mkm: M11DedicatedApp): String = {
    s"""{"error": s"Returned HTTP code ${mkm.responseCode}, exception: ${errorText(mkm.lastError)}"}"""
  }

  def getConfirmation(xml: String, tagName: String, entriesInRequest: Seq[SellerDataChanged], added: Boolean): String = {
    val ex = nativeProvider.saveToFile("xml-" + tagName + "-" + System.currentTimeMillis + ".xml", xml, null)
    if (ex == null) {
      println("Failed writing info xml: " + ex)
    }

    val tags = try {
      xmlList(getXml(xml).getElementsByTagName(tagName))
    } catch {
      case x: Throwable =>
        handleEx(x, "performing getXml from " + xml)
        Nil
    }

    val buf = new ListBuffer[SellerDataChanged]
    buf.append(entriesInRequest: _*)

    val xmlItems: Seq[ImportConfirmation] = tags.flatMap { x =>
      val xs = xmlList(x.getChildNodes)
      val success = java.lang.Boolean.parseBoolean(xs.find(_.getNodeName == "success").get.getTextContent)
      val message = xs.find(_.getNodeName == "message").map(_.getTextContent)
      val value = xs.find(_.getNodeName == "idArticle")
      // val englishName = xs.find(_.getNodeName == "engName").map(_.getTextContent)

      val ch = value.toList.flatMap(x => xmlList(x.getChildNodes))
      val idArticle =
        if (ch.size == 1 && ch.head.getNodeType == Node.TEXT_NODE) {
          ch.head.getTextContent.toLong
        } else {
          ch.find(_.getNodeName == "idArticle").map(_.getTextContent.toLong).getOrElse(-1)
        }

      val index = buf.indexWhere(b => b.externalId == idArticle)

      val collId: java.lang.Long =
        if (index == -1) {
          println("The element with id " + idArticle + " is not fund in mkm xml, success was: " + success + ", message: " + message)
          -1l
        } else {
          val item = buf(index)
          // We remove the element after finding it to prevent duplicate findings
          // (one idArticle maps to many collection ids)
          buf.remove(index)
          item.collectionId
        }

      val mapEntry =
        if (collId == -1) {
          None
        } else {
          if (message.isDefined) {
            Some(ImportConfirmation(collId, success, added, message.get))
          } else {
            Some(ImportConfirmation(collId, success, added, null))
          }
        }
      mapEntry
    }

    // If there are entries left without a matching item, they got a new articleId and must be removed and (in the next sync) new inserted to snapcardster
    val needToRemove: Array[ImportConfirmation] =
      if (added) {
        entriesInRequest.filter(e =>
          !xmlItems.exists(i =>
            i.collectionId == (if (e.collectionId == null) -1 else e.collectionId)
          )).map { x =>
          ImportConfirmation(
            if (x.collectionId == null) -1 else x.collectionId,
            successful = false,
            added = added,
            "needToRemoveFromSnapcardster"
          )
        }.toArray
      } else {
        Array()
      }
    new Gson().toJson((xmlItems ++ needToRemove).toArray)
  }

  override def getThread: Thread = thread

  override def getProperties: Properties = prop

  override def getAborted: BooleanProperty = aborted

  override def getRunning: BooleanProperty = running

  override def getMkmAppToken: StringProperty = mkmAppToken

  override def getMkmAppSecret: StringProperty = mkmAppSecret

  override def getMkmAccessToken: StringProperty = mkmAccessToken

  override def getMkmAccessTokenSecret: StringProperty = mkmAccessTokenSecret

  override def getSnapUser: StringProperty = snapUser

  override def getSnapPassword: StringProperty = snapPassword

  override def getSnapToken: StringProperty = snapToken

  override def getOutput: StringProperty = output

  override def getInterval: IntegerProperty = interval

  override def getnextSync(): IntegerProperty = nextSync
}
