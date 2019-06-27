package omnimtg

import omnimtg.Interfaces._
import java.io._
import java.nio.file._
import java.util
import java.util.regex._
import java.util.zip._
import java.util.{Date, Properties}
import com.google.gson._

import javax.xml.parsers.DocumentBuilderFactory
// import org.apache.commons.lang3.StringUtils
import org.w3c.dom.{Document, Node, NodeList}

import scala.collection.mutable.ListBuffer

case class LogItem(timestamp: Long, text: String, deleted: List[String], changed: List[String], added: List[String])

class MainController(propFactory: PropertyFactory, nativeProvider: NativeFunctionProvider) extends MainControllerInterface {
  def println(x: Any): Unit = {
    nativeProvider.println(x)
  }

  val title: String = "Omni MTG Sync Tool 2019-05-29 [H]"

  // when scryfall deployed: 3, before: 2
  val snapApiVersion: String = "3"

  // TODO: change back to test after test
  var snapBaseUrl: String = "https://api.snapcardster.com"
  //val snapBaseUrl: String = "https://dev.snapcardster.com"
  //val snapBaseUrl: String =  "https://test.snapcardster.com"
  //val snapBaseUrl: String = "http://localhost:9000"

  val CHANGED: String = "changed"
  val ADDED: String = "added"
  val REMOVED: String = "removed"
  val RESERVED: String = "reserved"

  def snapCsvEndpoint: String = snapBaseUrl + s"/importer/sellerdata/from/csv/$snapApiVersion"

  def snapLoginEndpoint: String = snapBaseUrl + "/auth"

  def snapChangedEndpoint: String = snapBaseUrl + s"/marketplace/sellerdata/changed/$snapApiVersion"

  val mkmBaseUrl: String = "https://api.cardmarket.com/ws/v2.0"
  val mkmStockEndpoint: String = mkmBaseUrl + "/stock"
  val mkmStockFileEndpoint: String = mkmBaseUrl + "/output.json/stock/file"

  private var thread: Thread = null
  private val prop: Properties = new Properties
  private val backupPath: Path = null
  //Paths.get("mkm_backup_" + System.currentTimeMillis() + ".csv")
  private val aborted: BooleanProperty = propFactory.newBooleanProperty(false)
  private val running: BooleanProperty = propFactory.newBooleanProperty(false)
  private val inSync: BooleanProperty = propFactory.newBooleanProperty(false)
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
  private val request: ObjectProperty = propFactory.newObjectProperty(null)
  private val logs: ObjectProperty = propFactory.newObjectProperty(Nil)
  //private var backupFirst = true
  private var addedList: List[String] = Nil
  private var changedList: List[String] = Nil
  private var deletedList: List[String] = Nil

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

  // called by server, ENV-Var to .properties logic
  def startServer(nativeBase: Object): Unit = {
    val env = System.getenv()
    val keys = Seq("mkmApp", "mkmAppSecret", "mkmAccessToken", "mkmAccessTokenSecret", "snapUser", "snapToken")
    var containedOne = false
    keys.foreach { k =>

      if (env.containsKey(k)) {
        val x = env.get(k)
        if (x != null && x != "") {
          containedOne = true
          prop.setProperty(k, x)
        }
      }
    }
    if (containedOne) {
      save(null)
    }

    if (env.containsKey("snapBaseUrl")) {
      val x = env.get("snapBaseUrl")
      if (x != null && x != "") {
        snapBaseUrl = x
        println("snapBaseUrl was set to " + snapBaseUrl)
      }
    }

    readProperties(nativeBase)

    if (keys.forall(k => Option(prop.getProperty(k)).getOrElse("").nonEmpty)) {
      running.setValue(true)
      val str = title + "\nAll values were set in prop, autostarted. The properties file seems to be ok."
      println(str)
      output.setValue(str)
    } else {
      val str = title + "\nNot all values were set in prop, no autostart. You can check the properties file."
      println(str)
      output.setValue(str)
    }

    if (env.containsKey("verbose")) {
      val x = env.get("verbose")
      if (x != null && x != "") {
        Config.setVerbose(java.lang.Boolean.parseBoolean(x))
        println("Config verbose was set to " + Config.isVerbose)
      }
    }

    if (env.containsKey("timeout")) {
      val x = env.get("timeout")
      if (x != null && x != "") {
        Config.setTimeout(x.toInt)
        println("Config timeout was set to " + Config.getTimeout)
      }
    }

    thread = run(nativeBase)
  }

  def save(nativeBase: Object): Unit = {
    println("save props")
    val ex = nativeProvider.save(prop, nativeBase)
    if (ex != null) {
      handleEx(ex)
    }
  }

  def snapConnector = new SnapConnector(nativeProvider)

  def loginSnap(): Unit = {
    output.setValue(outputPrefix() + "Logging in to Snapcardster...")
    val body = s"""{\"userId\":\"${snapUser.getValue}\",\"password\":\"${snapPassword.getValue}\"}"""
    output.setValue(outputPrefix() + body)
    val res = snapConnector.call(snapLoginEndpoint, "POST", body = body)
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
              inSync.setValue(true)
              sync(nativeBase)
              inSync.setValue(false)
            } catch {
              case e: Exception =>
                handleEx(e)
                inSync.setValue(false)
            }
            request.getValue match {
              case r: Runnable => r.run
              case null => () // ok, runnable or nothing
              case x => println("request was " + x + ", expected Runnable")
            }

            val seconds = interval.getValue.intValue
            nativeProvider.println("- Waiting " + seconds + " seconds until next sync...")
            for (x <- 1.to(seconds)) {
              // don't abort wait if changed during wait
              //if (seconds == interval.getValue.intValue) {
              nextSync.setValue(seconds - x)
              Thread.sleep(1000)
              if (x % 10 == 0) {
                nativeProvider.println(" - Waited " + x + " seconds, out of " + seconds + "  until next sync...")
              }
              //} else {
              //Thread.sleep(1000)
              //}
            }
          } else {
            // not running, just wait
            Thread.sleep(1000)
          }
        }
      }
    })
    t.start()
    t
  }

  def sync(nativeBase: Object): Unit = {
    //if (backupFirst) {
    // Create a local backup of the MKM Stock
    output.setValue(outputPrefix() + "Saving Backup of MKM Stock before doing anything")
    var csv = loadMkmStock()
    val bFile = s"backup_${System.currentTimeMillis}.csv"
    val saveBackupPath = new File(File.separatorChar + "backup" + File.separatorChar + bFile) //Paths.get("backup", bFile).toFile
    try {
      saveBackupPath.getParentFile.mkdirs
    } catch {
      case e: Exception => println(e)
    }
    val saveBackupPathAbsolute = saveBackupPath.getAbsolutePath
    val e = nativeProvider.saveToFile(saveBackupPathAbsolute, csv, nativeBase)
    if (e != null) {
      println(e)
    }

    //backupFirst = false
    //}

    val sb = new StringBuilder
    output.setValue(outputPrefix() + "Load snap changes...")

    // result of this method is already appended to output:
    loadSnapChangedAndDeleteFromStock(sb)

    sb.append("Loading MKM stock...\n")
    csv = loadMkmStock()
    sb.append("  " + csv.count(x => x == '\n') + " lines read from mkm stock\n")
    output.setValue(sb.toString)

    val res = postToSnap(csv)

    // output.setValue(outputPrefix() + snapCsvEndpoint + "\n" + res)
    println("res has a length of " + res.length)
    val items = getChangeItems(res)

    val info =
      if (items.isEmpty)
        "  No changes were done on the server, everything up to date"
      else
        readableChanges(items)

    sb.append("• MKM to Snapcardster, changes at Snapcardster:\n" + info)
    output.setValue(sb.toString)

    addLogEntry
  }

  def getLogs: List[LogItem] = {
    LogItem(System.currentTimeMillis, "Latest response: \n" + output.getValue, Nil, Nil, Nil) :: logs.getValue.asInstanceOf[List[LogItem]]
  }

  def addLogEntry: Unit = {
    if (deletedList.nonEmpty || changedList.nonEmpty || addedList.nonEmpty) {
      val item = LogItem(System.currentTimeMillis, output.getValue, deletedList, changedList, addedList)
      logs.setValue(item :: logs.getValue.asInstanceOf[List[LogItem]])
    }
  }

  def readableChanges(items: Seq[SellerDataChanged]): String = {
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
      if (parts.`type` == REMOVED || parts.`type` == RESERVED) {
        List(parts)
      } else {
        Nil
      }
    }

    val readableRemove = readableChanges(removedOrReservedItems)
    info.append(
      "Will remove " + removedOrReservedItems.length + " items...\n"
        + readableRemove + "\n"
    )

    deletedList = makeLogList(removedOrReservedItems)
    addedList = makeLogList(list.filter(_.`type` == ADDED))
    changedList = makeLogList(list.filter(_.`type` == CHANGED))

    output.setValue(info.toString)

    val resDel = deleteFromMkmStock(removedOrReservedItems)
    info.append(resDel + "\n")
    output.setValue(info.toString)

    var body = if (resDel.isEmpty) "[]" else resDel
    var res = snapConnector.call(snapChangedEndpoint, "POST", getAuth, body)
    info.append("  " + res + "\n")
    output.setValue(info.toString)

    val addedItems = list.flatMap { parts =>
      if (parts.`type` == ADDED || parts.`type` == CHANGED) {
        List(parts)
      } else {
        Nil
      }
    }

    val addedReadable = readableChanges(addedItems)
    info.append(
      "Will add " + addedItems.length + " items...\n"
        + addedReadable + "\n"
    )

    output.setValue(info.toString)

    val resAdd = addToMkmStock(addedItems)
    info.append("  " + resAdd + "\n")
    output.setValue(info.toString)

    info.append("Notify snapcardster...\n")
    output.setValue(info.toString)

    body = if (resAdd.isEmpty) "[]" else resAdd
    res = snapConnector.call(snapChangedEndpoint, "POST", getAuth, body)

    info.append("  " + res + "\n")
    output.setValue(info.toString)

    info
  }

  def makeLogList(seq: Seq[SellerDataChanged]): List[String] = {
    seq.map { x =>
      val csv = x.info
      x.`type` + " " + csv.name +
        " (" + csv.editionCode + ") " + csv.language.shortString + " " +
        csv.condition.shortString + (if (csv.foil) " Foil" else "") +
        (if (csv.altered) " Altered" else "") +
        (if (csv.signed) " Signed" else "") +
        " " + csv.price + "€"
    }
      .sortBy(x => x)
      .groupBy(x => x).toList.map(x => "  " + x._2.length + " " + x._1)
      .toList
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
      println(e)
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
    val res = snapConnector.call(snapCsvEndpoint, "POST", getAuth, body)
    res
  }

  def loadChangedFromSnap(): String = {
    val res = snapConnector.call(snapChangedEndpoint, "GET", getAuth, null)
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
      val obj = new Gson().fromJson(mkm.responseContent, classOf[MKMSomething])
      //      System.out.println(obj.toString)
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
      val csv = content.toArray.mkString("\n") // StringUtils.join(content, "\n")
      csv
    } else {
      var text = "Error:" + mkmStockFileEndpoint + " had server response: " + mkm.responseCode + " "
      if (mkm.lastError != null) {
        text += mkm.lastError.toString
      }
      output.setValue(text)
      sys.error(text)
    }
  }

  def getMkm: M11DedicatedApp = {
    val x = new M11DedicatedApp(mkmAppToken.getValue, mkmAppSecret.getValue, mkmAccessToken.getValue, mkmAccessTokenSecret.getValue, nativeProvider)
    x.setDebug(true)
    x
  }

  def deleteFromMkmStock(entries: Seq[SellerDataChanged]): String = {
    val buf = ListBuffer[ImportConfirmation]()
    for (seq <- entries.grouped(100)) {
      deleteFromMkmStockWindowedRaw(seq) match {
        case Left(x) => return x
        case Right(res) => buf ++= res
      }
    }
    new Gson().toJson(buf.toList.toArray)
  }

  def deleteFromMkmStockWindowedRaw(entries: Seq[SellerDataChanged]): Either[String, Array[ImportConfirmation]] = {
    if (entries.isEmpty)
      return Right(Array())

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
      Left(getErrorString(mkm))
    } else {
      Right(getConfirmationRaw(mkm.responseContent, "deleted", entries, added = false))
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
    new Gson().toJson(getConfirmationRaw(xml, tagName, entriesInRequest, added))
  }

  def getConfirmationRaw(xml: String, tagName: String, entriesInRequest: Seq[SellerDataChanged], added: Boolean): Array[ImportConfirmation] = {
    val ex = nativeProvider.saveToFile(
      File.separatorChar + "logs" + File.separatorChar + "xml-" + tagName + "-" + System.currentTimeMillis + ".xml", xml, null
    )
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
    (xmlItems ++ needToRemove).toArray
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

  override def getNextSync: IntegerProperty = nextSync

  override def getInSync: BooleanProperty = inSync

  override def getRequest: ObjectProperty = request

  def getLog: ObjectProperty = logs
}
