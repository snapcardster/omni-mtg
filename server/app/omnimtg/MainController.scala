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

import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Success, Try}
// import org.apache.commons.lang3.StringUtils
import org.w3c.dom.{Document, Node, NodeList}

import scala.collection.mutable.ListBuffer

case class LogItem(timestamp: Long, text: String, deleted: List[String], changed: List[String], added: List[String])

class MainController(
                      val propFactory: PropertyFactory,
                      val nativeProvider: NativeFunctionProvider
                    ) extends MainControllerInterface {
  val title: String = "OmniMtg 2020-02-26"
  // TODO update version

  def saveProps = {
    println("Saving Props")
    val x = nativeProvider.updatePropertiesFromPropsAndSaveToFile(prop, this, null)
    if (x != null) {
      handleEx(x, "updatePropertiesFromPropsAndSaveToFile")
    }
  }

  val REMOVE_FROM_CSV: String = "REMOVE_FROM_CSV"
  val removeIfCommentContainsUnique = "#unique"

  // CSV indices
  val IDPRODUCT = 1
  val COMMENT = 14

  // constant
  val problemCardEditions = List(
    "Fallen Empires", "Antiquities", "Homelands", "Alliances", "Chronicles"
  )
  val problemCardNames = List(
    "Brothers Yamazaki",
    "Izzet Guildgate",
    "Dimir Guildgate",
    "Selesnya Guildgate",
    "Simic Guildgate",
    "Azorius Guildgate",
    "Boros Guildgate",
    "Golgari Guildgate",
    "Gruul Guildgate",
    "Orzhov Guildgate",
    "Rakdos Guildgate"
  )

  val splitter = "\";\""

  val leaveOutCards = List(
    "Mountain",
    "Island",
    "Plains",
    "Forest",
    "Swamp"
  )

  // when scryfall deployed: 3, before: 2
  val snapApiVersion: String = "3"

  val CHANGED: String = "changed"
  val ADDED: String = "added"
  val REMOVED: String = "removed"
  val RESERVED: String = "reserved"

  val mkmBaseUrl: String = "https://api.cardmarket.com/ws/v2.0"
  val mkmStockEndpoint: String = mkmBaseUrl + "/stock"
  val mkmProductEndpoint: String = mkmBaseUrl + "/products"
  val mkmStockFileEndpoint: String = mkmBaseUrl + "/output.json/stock/file"
  val mkmProductFileEndpoint: String = mkmBaseUrl + "/output.json/productlist"


  def snapCsvEndpoint: String = snapBaseUrl + s"/importer/sellerdata/from/csv/$snapApiVersion"

  def snapCsvBidEndpoint: String = snapBaseUrl + s"/importer/sellerdata/bidsFromCsv/$snapApiVersion"

  def snapLoginEndpoint: String = snapBaseUrl + "/auth"

  def snapChangedEndpoint: String = snapBaseUrl + s"/marketplace/sellerdata/changed/$snapApiVersion"

  // mutable

  //var lastBidCreateOptions: String = ""

  // TODO: change back to test after test
  var snapBaseUrl: String = "https://api.snapcardster.com"
  //var snapBaseUrl: String = "https://dev.snapcardster.com"
  //var snapBaseUrl: String =  "https://api2.snapcardster.de"
  //var snapBaseUrl: String = "http://localhost:9000"

  var bidLanguages: List[Int] = Nil
  var bidConditions: List[Int] = Nil
  var bidFoils: List[Boolean] = Nil

  def println(x: Any): Unit = {
    nativeProvider.println(x match {
      case x: Exception => x.toString + "[" + x.getStackTrace.take(5).mkString(", ") + "]"
      case x => x
    })
  }

  def mkmProductExpansionEndpoint(idGame: String = "1"): String =
    mkmBaseUrl + "/output.json/games/" + idGame + "/expansions"

  private var thread: Thread = null

  override def getThread = thread

  private var prop: Properties = new Properties()

  override def getProperties = prop

  private val backupPath: Path = null
  //Paths.get("mkm_backup_" + System.currentTimeMillis() + ".csv")
  val aborted: BooleanProperty = propFactory.newBooleanProperty(false)
  val running: BooleanProperty = propFactory.newBooleanProperty(false)
  val inSync: BooleanProperty = propFactory.newBooleanProperty(false)
  val mkmAppToken: StringProperty = propFactory.newStringProperty("mkmApp", "", prop)
  val mkmAppSecret: StringProperty = propFactory.newStringProperty("mkmAppSecret", "", prop)
  val mkmAccessToken: StringProperty = propFactory.newStringProperty("mkmAccessToken", "", prop)
  val mkmAccessTokenSecret: StringProperty = propFactory.newStringProperty("mkmAccessTokenSecret", "", prop)
  val snapUser: StringProperty = propFactory.newStringProperty("snapUser", "", prop)

  val bidPriceMultiplier: DoubleProperty = propFactory.newDoubleProperty("bidPriceMultiplier", 0.0, prop)
  val minBidPrice: DoubleProperty = propFactory.newDoubleProperty("minBidPrice", 0.0, prop)
  val maxBidPrice: DoubleProperty = propFactory.newDoubleProperty("maxBidPrice", 0.0, prop)
  val askPriceMultiplier: DoubleProperty = propFactory.newDoubleProperty("askPriceMultiplier", 1.0, prop)
  val snapCallsSoFar: IntegerProperty = propFactory.newIntegerProperty("snapCallsSoFar", 0, prop)
  val mkmCallsSoFar: IntegerProperty = propFactory.newIntegerProperty("snapCallsSoFar", 0, prop)

  val snapPassword: StringProperty = propFactory.newStringProperty("snapPassword", "", prop)
  val snapToken: StringProperty = propFactory.newStringProperty("snapToken", "", prop)
  val output: StringProperty = propFactory.newStringProperty("Output appears here. Click Start Sync to start. This requires valid api data.")
  val interval: IntegerProperty = propFactory.newIntegerProperty("interval", 180, prop)
  val nextSync: IntegerProperty = propFactory.newIntegerProperty(0)
  val request: ObjectProperty = propFactory.newObjectProperty(null)
  val logs: ObjectProperty = propFactory.newObjectProperty(Nil)
  //private var backupFirst = true

  var addedList: List[String] = Nil
  var changedList: List[String] = Nil
  var deletedList: List[String] = Nil

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
    //readProperties(nativeBase)
    //running.setValue(true)
    //thread = run(nativeBase)
    sys.error("not used start")
  }

  // called by server, ENV-Var to .properties logic
  def startServer(nativeBase: Object): Unit = {

    // load everything, incl multipliers etc
    readProperties(nativeBase)

    val env = System.getenv()
    val keys = Seq("mkmApp", "mkmAppSecret", "mkmAccessToken", "mkmAccessTokenSecret", "snapUser", "snapToken")
    var containedOne = false
    keys.foreach { k =>
      if (env.containsKey(k)) {
        val x = env.get(k)
        if (x != null && x != "") {
          containedOne = true
          // update from env
          prop.setProperty(k, x)
        }
      }
    }

    if (containedOne) {
      // then save if there are any from env
      savePropertiesToFile(nativeBase)
    }

    if (env.containsKey("snapBaseUrl")) {
      val x = env.get("snapBaseUrl")
      if (x != null && x != "") {
        snapBaseUrl = x
        println("snapBaseUrl was set to " + snapBaseUrl)
      }
    }

    println("snapBaseUrl is " + snapBaseUrl)
    println("bidOptions (bidPriceMultiplier: " + bidPriceMultiplier.getValue + ", bidLanguages=" + bidLanguages + ")")

    val keysWithLen = keys.map(k => k + "-size: " + Option(prop.getProperty(k)).getOrElse("").length).mkString(", ")

    if (keys.forall(k => Option(prop.getProperty(k)).getOrElse("").nonEmpty)) {
      running.setValue(true)
      val str = title + s"\nAll values were set in prop ${keysWithLen}, autostarted. The properties file seems to be ok."
      println(str)
      output.setValue(str)
    } else {
      val str = title + s"\nNot all values were set in prop ${keysWithLen}, no autostart. You can check the properties file."
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

  def savePropertiesToFile(nativeBase: Object): Unit = {
    println("savePropertiesToFile save props keys: " + prop.keySet().toArray.mkString(", "))
    val ex = nativeProvider.savePropertiesToFile(prop, nativeBase)
    if (ex != null) {
      handleEx(ex)
    }
  }

  def snapConnector = new SnapConnector(nativeProvider)

  def loginSnap(): Unit = {
    output.setValue(outputPrefix() + "Logging in to Mage...")
    val body = s"""{\"userId\":\"${snapUser.getValue}\",\"password\":\"${snapPassword.getValue}\"}"""
    output.setValue(outputPrefix() + body)
    val res = snapConnector.call(this, snapLoginEndpoint, "POST", body = body)
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
    //running.setValue(false)
    //thread.interrupt()

    sys.error("not used stop")
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
            } catch {
              case e: Exception =>
                handleEx(e)
              case e: Error =>
                handleEx(e)
                System.exit(1111)
            }
            inSync.setValue(false)

            request.getValue match {
              case r: Runnable => r.run()
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
            Thread.sleep(10000)
            nativeProvider.println(" - Not running, just wait")
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
    var csv = loadMkmStock(getMkm)
    val bFile = s"backup_${System.currentTimeMillis}.csv"
    val saveBackupPath = new File(File.separatorChar + "backup" + File.separatorChar + bFile) //Paths.get("backup", bFile).toFile
    try {
      saveBackupPath.getParentFile.mkdirs
    } catch {
      case e: Exception => println(e)
    }
    val saveBackupPathAbsolute = saveBackupPath.getAbsolutePath
    val e = nativeProvider.saveToFile(saveBackupPathAbsolute, csv.mkString("\n"), nativeBase)
    if (e != null) {
      println(e)
    }

    //backupFirst = false
    //}

    val sb = new StringBuilder
    val res1 = loadSnapChangedAndDeleteFromStock(sb)

    sb.append("Sync run " + new Date() + ". Loading MKM stock...\n")
    csv = loadMkmStock(getMkm)
    sb.append("  " + csv.length + " lines read from mkm stock\nPosting to Mage (" + new Date() + ")...")
    output.setValue(sb.toString)
    val start = System.currentTimeMillis
    val res = postToSnap(csv.mkString("\n"))

    val time = System.currentTimeMillis - start
    // output.setValue(outputPrefix() + snapCsvEndpoint + "\n" + res)
    println("res has a length of " + res.length)
    val items = getChangeItems(res)

    val info =
      if (items.isEmpty)
        "  No changes were done on the server, everything up to date"
      else
        readableChanges(items)

    sb.append("• MKM to Mage, changes at Mage (took " + (time / 1000.0) + "s):\n").append(info)

    val (infoBids, resBids) = postToSnapBids(csv)
    csv = null

    val bidInfo = "Bid csv transferred: " + resBids + " (" + infoBids + ")"

    sb.append("\n" + bidInfo)
    output.setValue(sb.toString)

    addLogEntry(infoBids, resBids)
  }

  def getLogs: List[LogItem] = {
    LogItem(System.currentTimeMillis,
      output.getValue, Nil, Nil, Nil) ::
      logs.getValue.asInstanceOf[List[LogItem]]
  }

  // "Latest response: \n" +

  def addLogEntry(infoBids: String, resBids: Int): Unit = {

    val item = currentLogItem

    if (deletedList.nonEmpty || changedList.nonEmpty || addedList.nonEmpty) {
      logs.setValue(item :: Nil)
      // :: logs.getValue.asInstanceOf[List[LogItem]])
    } /*else if (resBids != 0) {
      logs.setValue(item :: logs.getValue.asInstanceOf[List[LogItem]])
    }*/
  }

  def currentLogItem() = {
    LogItem(System.currentTimeMillis,
      "", deletedList, changedList, addedList)
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

  val alreadyAddedSet =
    mutable.HashSet[Long]()

  def loadSnapChangedAndDeleteFromStock(info: StringBuilder): StringBuilder = {
    info.append(outputPrefix() + "• Mage to MKM, changes at MKM:\n")
    val json = loadChangedFromSnap()

    println(snapChangedEndpoint + "\n" + json)

    val list: Array[SellerDataChanged] = getChangeItems(json)

    val removedOrReservedItems: Array[SellerDataChanged] =
      list.filter { parts =>
        parts.`type` == REMOVED || parts.`type` == RESERVED
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

    if (resDel.isSuccess) {
      val body = if (resDel.get.isEmpty) "[]" else resDel.get
      val res = snapConnector.call(this, snapChangedEndpoint, "POST", getAuth, body)
      info.append("  " + res + "\n")
      output.setValue(info.toString)
    } else {
      println("Error: deleteFromMkmStock for " + snapUser.getValue + " => " + resDel.failed.get)
    }

    val addedItems0 =
      list.flatMap { parts =>
        if (parts.`type` == ADDED || parts.`type` == CHANGED) {
          List(parts)
        } else {
          Nil
        }
      }

    val (alreadyAddedItems: Array[SellerDataChanged],
    addedItems: Array[SellerDataChanged]) =
      addedItems0.partition(x =>
        alreadyAddedSet.contains(
          x.collectionId
        )
      )

    addedItems0.foreach(x =>
      alreadyAddedSet.add(
        x.collectionId
      )
    )

    info.append(
      "Found already added (thus skipped) " + alreadyAddedItems.length + " items...\n"
        + readableChanges(alreadyAddedItems) + "\n"
    )

    val addedReadable = readableChanges(addedItems)
    info.append(
      "Will add " + addedItems.length + " items...\n"
        + addedReadable + "\n"
    )

    output.setValue(info.toString)

    val resAdd =
      addToMkmStock(addedItems, getMkm)
    info.append("  " + resAdd + "\n")
    output.setValue(info.toString)

    info.append("Notify Mage...\n")
    output.setValue(info.toString)

    val body = if (resAdd.isEmpty) "[]" else resAdd
    val res = snapConnector.call(this, snapChangedEndpoint, "POST", getAuth, body)

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
      case x: Exception =>
        sys.error("getChangeItems: " + x.toString + "\n" + json)
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

  def filterBids(csv: Array[String]): Array[String] = {
    val bidPriceMultiplierValue: Double = Double.unbox(bidPriceMultiplier.getValue)
    val minBidPriceValue: Double = Double.unbox(minBidPrice.getValue)
    val maxBidPriceValue: Double = Double.unbox(maxBidPrice.getValue)
    val empty: Array[String] = Array()

    //    Predef.println("inspect lines" + csv.length)

    csv.flatMap { line =>
      val parts = line.split(splitter)

      val idProduct = parts(1)
      if (idProduct == "idProduct") {
        Array(line)
      } else {
        //Predef.println("inspect line" + line)
        val price = parts(6)
        //Predef.println("as" + line + " : " + price)
        //println(price + "(" + line + ")")
        var resultPrice = price.toDouble * bidPriceMultiplierValue
        if (resultPrice < 0.01) {
          resultPrice = 0.01
        }
        if (minBidPriceValue <= resultPrice && resultPrice <= maxBidPriceValue) {
          val languageString = parts(7)
          val conditionString = parts(8)
          val foilString = parts(9)

          //"583700611";"366171";"Liliana of the Veil";"Liliana of the Veil";"";"Ultimate Masters";"100.00";"1";"NM";"";"";"";"";"";"1";"1";""
          val language = ParseMkm.parseLanguage(languageString)
          val condition = ParseMkm.parseCondition(conditionString)
          val foil = ParseMkm.parseFoil(foilString)

          if (bidFoils.contains(foil) && bidLanguages.contains(language) && bidConditions.contains(condition)) {
            /*
                0;          1;             2;           3;     4;          5;      6; ...
      "idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale";"Collector Number"
      "561339329";"399979";"Murderous Rider // Swift End";"Murderous Rider // Swift End";"ELD";"Throne of Eldraine";"22.00";"1";"NM";"";"";"";"";"";"1";"1";"97"
           */
            val partsReplaced = parts.zipWithIndex.map { case (x, i) =>
              if (i == 6) { // change price, two digits
                (Math.floor(resultPrice * 100) / 100.0).toString
              } else {
                x
              }
            }
            Array(partsReplaced.mkString(splitter))
          } else {
            empty
          }
        } else {
          empty
        }
      }
    }
  }

  def postToSnapBids(csv: Array[String]): (String, Int) = {
    val csvLength = csv.length
    val bidPriceMultiplierValue: Double = Double.unbox(bidPriceMultiplier.getValue)
    val minBidPriceValue: Double = Double.unbox(minBidPrice.getValue)
    val maxBidPriceValue: Double = Double.unbox(maxBidPrice.getValue)
    if (bidPriceMultiplierValue > 0) {
      try {
        val lines = filterBids(csv)
        //csv = null
        val body = new Gson().toJson(MtgCsvFileRequest(
          "mkmStock.csv",
          lines.mkString("\n"),
          bidPriceMultiplier = 1.0,
          info = getInfo
          // we filter csv now here and change values
          // instead of letting the backend do that
        ))

        println("post bids: " + lines.length + " lines of csv, first card line:\n" + lines.drop(1).headOption.getOrElse(""))

        val res = snapConnector.call(this, snapCsvBidEndpoint, "POST", getAuth, body)
        val currentBidCreateOptions =
          "mult" + bidPriceMultiplierValue + ",min" + minBidPriceValue + ",max" + maxBidPriceValue +
            "bidFoils" + bidFoils + ",bidLanguages" + bidLanguages + "bidConditions" + bidConditions

        println("filter bids from " + csvLength + " to " + lines.length + " with " + currentBidCreateOptions)

        (res, lines.length - 1)
      } catch {
        case e: Exception =>
          handleEx(e, "postToSnapBids with mult " + bidPriceMultiplierValue)
          ("Error: " + e.getMessage, 0)
      }
    } else {
      ("bidPriceMultiplier was zero", 0)
    }
  }

  def postToSnap(csv: String): String = {
    val body = new Gson().toJson(MtgCsvFileRequest(
      "mkmStock.csv",
      csv,
      askPriceMultiplier = askPriceMultiplier.getValue,
      // we need ask csv unchanged in case it goes back
      info = getInfo
    ))
    //csv = null
    val res = snapConnector.call(this, snapCsvEndpoint, "POST", getAuth, body)
    res
  }

  def getInfo: String = {
    title + " (Ask: " + askPriceMultiplier.getValue + ", Bid: " + bidPriceMultiplier.getValue + "(" + minBidPrice.getValue + "," + maxBidPrice.getValue + ")/L" + bidLanguages.length + "/F" + bidFoils.length + "/C" + bidConditions.length + "/AlreadyAdded" + alreadyAddedSet.size + ")"
  }

  def loadChangedFromSnap(): String = {
    val res = snapConnector.call(this, snapChangedEndpoint, "GET", getAuth, null)
    res
  }

  def getAuth: String = {
    snapUser.getValue + "," + snapToken.getValue
  }

  def unzipB64StringAndGetLines(b64: String): Array[String] = {
    val arr: Array[Byte] = nativeProvider.decodeBase64(b64)
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
        /*if (line.contains("High Tide")) {
          println("VERSION " + line)
        }*/
        content.add(line)
      }
    }
    content.toArray(new Array[String](content.size))
  }

  def getCsvLines(mkmResponse: String): Array[String] = {
    val builder = new GsonBuilder
    val obj = new Gson().fromJson(mkmResponse, classOf[MKMSomething])
    //      System.out.println(obj.toString)
    val result = obj.stock

    // get string content from base64'd gzip
    unzipB64StringAndGetLines(result)
  }

  var idProductToCollectorNumber: mutable.Map[Long, String] =
    new mutable.HashMap[Long, String]

  def calcLookup(mkm: M11DedicatedApp, productIds: Set[Long]): Int = {
    productIds.toSeq.flatMap { id =>
      if (mkmReqTimoutable(mkmProductEndpoint + "/" + id, "GET", (url, method) =>
        mkm.request(url, method, null, null, true))) {
        val prodNode =
          xmlList(getXml(mkm.responseContent).getFirstChild.getChildNodes)
            .find(x => x.getNodeName == "product")
        val number =
          prodNode.flatMap { x =>
            xmlList(x.getChildNodes)
              .find(x => x.getNodeName == "number")
              .map(_.getTextContent)
          }

        val isVersionCard =
          prodNode.flatMap { x =>
            xmlList(x.getChildNodes)
              .find(x => x.getNodeName == "enName")
              .map(_.getTextContent.contains(" (Version "))
          }.getOrElse(false)

        if (Config.isVerbose) {
          println("Prod " + id + " => coll num <" + number + "> / version card " + isVersionCard)
        }
        if (isVersionCard) {
          idProductToCollectorNumber.put(id, REMOVE_FROM_CSV)
          None
        } else {
          number.map { num =>
            idProductToCollectorNumber.put(id, num)
            1
          }
        }
      } else {
        None
      }
    }.sum
  }

  def refreshLookupIfNeeded(mkm: M11DedicatedApp, csv: Array[String]): Unit = {
    val productIdsFromCsvThatNeedLookup =
      csv.flatMap { x =>
        val parts = x.split(splitter)
        val idProduct = Try(parts(IDPRODUCT).toLong)

        if (idProduct.isSuccess && ambiguousProductIdLookup.get.contains(idProduct.get)) {
          Some(idProduct.get)
        } else if (idProduct.isSuccess && (
          problemCardEditions.contains(parts(5)) || problemCardNames.contains(parts(2))
          )) {
          // idProductToCollectorNumber.put(idProduct.get, REMOVE_FROM_CSV)
          if (Config.isVerbose) {
            println("Problem Set/Card, will investigate card " + parts(2) + " (" + parts(5) + ")")
          }
          Some(idProduct.get)
        } else {
          None
        }
      }.toSet

    val diff = productIdsFromCsvThatNeedLookup.diff(idProductToCollectorNumber.keySet)
    if (diff.nonEmpty) {
      val newEntries = calcLookup(mkm, diff)
      println("collectorNumber lookup: newEntries=" + newEntries + ", total: " + idProductToCollectorNumber.size)
    } else {
      println("collectorNumber lookup: diff was empty, total: " + idProductToCollectorNumber.size)
    }

    /*val doc = getXml(xmlDocString)
    val response = doc.getChildNodes.item(0)
    val xml = xmlList(response.getChildNodes)
    val idArticleToCollectorNumber =
      xml.flatMap { x =>
        val nodes = xmlList(x.getChildNodes)
        val subNodes =
          nodes.filter(x => x.getNodeName == "idArticle" || x.getNodeName == "product")

        subNodes.find(_.getNodeName == "idArticle").map(_.getTextContent).flatMap { id =>
          subNodes.find(_.getNodeName == "product").flatMap(x => xmlList(x.getChildNodes).find(_.getNodeName == "nr")
            .map(_.getTextContent)).map(collectorNumber =>
            id -> collectorNumber
          )
        }
      }.toMap*/
  }

  def getCsvWithProductInfo(mkm: M11DedicatedApp): Option[Array[String]] = {
    if (mkmReqTimoutable(mkmStockFileEndpoint, "GET", (url, method) =>
      mkm.request(url, method, null, null, true))) {
      val jsonWithCsv = mkm.responseContent
      // returns product xml within result
      // BUT: works only with < 19800 offers, returns 404 with more / or maybe there are specific cards that cause this...

      val csv = getCsvLines(jsonWithCsv)
      refreshLookupIfNeeded(mkm, csv)
      return Some(buildNewCsv(csv))
    }
    None
  }

  var ambiguousProductIdLookup: Option[Map[Long, Seq[String]]] = None

  var oversizedNames: Set[String] = Set.empty
  var oversizedNamesAge: Long = 0

  def loadMkmStock(mkm: M11DedicatedApp): Array[String] = {

    oversizedNamesAge += 1
    if (oversizedNamesAge > 100 || oversizedNames.isEmpty) {
      oversizedNames = getOversizedCardNames.toSet
      oversizedNamesAge = 0
    }
    // returns csv
    /*
    Tue Nov 05 12:44:35 UTC 2019 > Requesting GET https://api.cardmarket.com/ws/v2.0/output.json/stock/file
    Tue Nov 05 12:44:35 UTC 2019 > Response Code is 200
    Tue Nov 05 12:44:35 UTC 2019 > Requesting GET https://api.cardmarket.com/ws/v2.0/stock
    Tue Nov 05 12:44:35 UTC 2019 > Response Code is 404
     */
    if (ambiguousProductIdLookup.isEmpty) {
      ambiguousProductIdLookup = getAmbigousProductIds(mkm)
    }

    getCsvWithProductInfo(mkm) match {
      case Some(x) => return x
      case _ => ()
    }

    var text = "Error:" + mkmStockFileEndpoint + " had server response: " + mkm.responseCode + " "
    if (mkm.lastError != null) {
      text += mkm.lastError.toString
    }
    output.setValue(text)
    sys.error(text)
  }

  // returns a map: ProductId -> extra/not-extra variants
  def getAmbigousProductIds(mkm: M11DedicatedApp): Option[Map[Long, Seq[String]]] = {

    if (mkmReqTimoutable(mkmProductExpansionEndpoint(), "GET", (url, method) =>
      mkm.request(url, method, null, null, true))) {

      val editionsJson = mkm.responseContent

      if (Config.isVerbose) {
        println("editionsJson" + editionsJson)
      }

      val expansions: MKMExpansions = new Gson().fromJson(editionsJson, classOf[MKMExpansions])
      //      System.out.println(obj.toString)
      if (Config.isVerbose) {
        println("editions" + expansions)
      }

      val expansionLookup: Map[Long, String] = expansions.expansion.map(x =>
        x.idExpansion -> x.enName
      ).toMap

      if (mkmReqTimoutable(mkmProductFileEndpoint, "GET", (url, method) =>
        mkm.request(url, method, null, null, true))) {
        val productJson = mkm.responseContent
        //println(productJson)

        val obj = new Gson().fromJson(productJson, classOf[MKMProductsfile])
        //      System.out.println(obj.toString)
        val result = obj.productsfile
        //          0,     1,            2,         3,             4,            5            6
        //"idProduct","Name","Category ID","Category","Expansion ID","Metacard ID","Date Added"
        //"1","Altar's Light","1","Magic Single","45","129","2007-01-01 00:00:00"
        val strings = unzipB64StringAndGetLines(result)

        // TODO: set to false, useful for debugging
        val karstenTest = false
        if (karstenTest) {
          val stream = new FileOutputStream(Paths.get("products.csv").toFile)
          stream.write(strings.mkString("\n").getBytes)
          stream.flush()
          stream.close()
        }

        val cardNameToSets = new mutable.HashMap[String, List[MKMProductEntry]]()
        strings.foreach { x =>
          if (x.contains("Magic Single")) {
            val parts = x.split("\",\"")
            val enName = parts(1)
            if (parts(4) != "") {
              Try(parts(4).toLong) match {
                case Failure(exception) =>
                  handleEx(exception, "productsfile part 4 line " + x)
                case Success(idExpansion) =>
                  Try(parts(0).substring(1).toLong) match {
                    case Failure(exception) =>
                      handleEx(exception, "productsfile part 0 line " + x)
                    case Success(idProd) =>
                      val entry =
                        MKMProductEntry(
                          idProduct = idProd,
                          enName = enName,
                          idExpansion = idExpansion,
                          expansionName = expansionLookup.getOrElse(idExpansion, "")
                        )
                      val list =
                        entry :: (cardNameToSets.get(enName) match {
                          case Some(value) => value
                          case None => Nil
                        })
                      cardNameToSets.put(enName, list)
                  }
              }
            }
          }
        }

        val withMoreThanOneSet = cardNameToSets.filter(_._2.size > 1)
        //+println(withMoreThanOneSet.mkString("\n"))

        val idProductToExpansions: Map[Long, List[String]] =
          withMoreThanOneSet.flatMap { case (key, value) =>
            value.flatMap { x =>
              value.find(y =>
                y.expansionName == x.expansionName + ": Extras" ||
                  // this fixes our mage card db error, we need the lookup anyway
                  y.expansionName == x.expansionName + ": Promos" ||
                  // jeez, also include the promo versions
                  y.expansionName == x.expansionName.replace(": Extras", "").replace(": Promos", "") + ": Promos").toList
                .flatMap(y =>
                  List(y.idProduct -> y.expansionName, x.idProduct -> x.expansionName)
                )
            } match {
              case Nil => Nil
              case seq =>
                seq.map(_._1).map(x => x -> seq.map(_._2).distinct)
            }
          }.toMap
        /*
        TODO: FOR TESTS
        return Some(
          cardNameToSets.values.flatMap(_.map(x => x.idProduct -> List(x.expansionName))).toMap)
        */
        return Some(idProductToExpansions)
      }
    }
    None
  }

  def getXmlStock(start: Int = 1): String = {
    val mkm = getMkm

    if (mkmReqTimoutable(mkmStockEndpoint + "/" + start, "GET", (url, method) =>
      mkm.request(url, method, null, null, true))) {
      val part = mkm.responseContent
      if (getXml(part).getFirstChild.getChildNodes.getLength == 100) {

        part + getXmlStock(start + 100)
      } else {
        part
      }
    } else {
      sys.error("request returned false, last error: " + mkm.lastError)
    }
  }

  def buildNewCsv(csv: Array[String]): Array[String] = {
    val csvWithCol = csv.flatMap { line =>
      val parts = line.split(splitter)
      val idProduct = parts(1)
      val cardName = parts(2)

      /*
          0;          1;             2;           3;     4;          5;      6;         7;          8;      9;       10;        11;        12;        13;      14;
"idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale";"Collector Number"
"561339329";"399979";"Murderous Rider // Swift End";"Murderous Rider // Swift End";"ELD";"Throne of Eldraine";"22.00";"1";"NM";"";"";"";"";"";"1";"1";"97"
           */
      val partsReplaced = parts.zipWithIndex.map { case (x, i) =>
        if (idProduct == "idProduct") {
          x // header unchanged
        } else {
          if (i == 4) { // exp code to empty
            ""
          } else if (i == 5) { // exp name without : Extras (confuses backend)
            x
              .replace(": Extras", "")
            // NO, we cannot do that (yet) .replace(": Promos", "")
          } else {
            x
          }
        }
      }
      val result = partsReplaced.mkString(splitter)
      if (
        leaveOutCards.contains(cardName) ||
          oversizedNames.contains(cardName)) {
        None
      } else if (idProduct == "idProduct") {
        Some(result + ";\"collectorNumber\"")
      } else {
        val collectorNumberMaybe = idProductToCollectorNumber.get(idProduct.toLong)
        if (parts(COMMENT).toLowerCase.contains(removeIfCommentContainsUnique)
          || collectorNumberMaybe.contains(REMOVE_FROM_CSV)) {
          None
        } else {
          Some(result + ";\"" + collectorNumberMaybe.getOrElse("") + "\"")
        }
      }
    }

    csvWithCol
  }

  def getMkm: M11DedicatedApp = {
    val x = new M11DedicatedApp(mkmAppToken.getValue, mkmAppSecret.getValue, mkmAccessToken.getValue, mkmAccessTokenSecret.getValue, nativeProvider)
    x.setDebug(true)
    x
  }

  def deleteFromMkmStock(entries: Seq[SellerDataChanged]): Try[String] = {
    val buf = ListBuffer[ImportConfirmation]()
    for (seq <- entries.grouped(100)) {
      deleteFromMkmStockWindowedRaw(seq) match {
        case Left(x) => return Failure(new IllegalStateException(x))
        case Right(res) => buf ++= res
      }
    }
    Success(new Gson().toJson(buf.toList.toArray))
  }

  def deleteFromMkmStockWindowedRaw(entries: Seq[SellerDataChanged]): Either[String, Array[ImportConfirmation]] = {
    if (entries.isEmpty)
      return Right(Array())

    val mkm = getMkm

    val entriesGrouped = entries.groupBy(_.externalId).toSeq
    val body = // NO SPACE at beginning, this is important
      s"""<?xml version="1.0" encoding="utf-8"?>
      <request>
      ${
        entriesGrouped.map(group =>
          s"""
            <article>
              <idArticle>${group._1}</idArticle>
              <count>${group._2.length}</count>
            </article>
            """
        ).mkString("")
      }
      </request>
      """

    val hasOutput = false
    if (!mkmReqTimoutable(mkmStockEndpoint, "DELETE", (url, method) =>
      mkm.request(url, method, body, "application/xml", hasOutput))) {
      handleEx(mkm.lastError, entries)

      // try again later, API may be blocked or down,
      // it's important to successfully delete the offers from the mkm stock,
      // otherwise we cannot guarantee unique sales, if there's an issue with cardmarket api,
      // we need to know, so I'll log these occurrences to keep track of such cases (happens above)
      Left(getErrorString(mkm))

      // If an api error should lead to remove from snap, this applies:

      // we cannot process an error here (or in the caller),
      // we should still respond to snap server with need to remove
      // even if cardmarket is fully offline, this will in result in a lot of deletes at mage
      // potentially, but since these are only soft deletes and they came from
      // purchases at mage in the first place, this is ok - they will be added again, if the sync is
      // online again. Short side note: If they should be deleted from mkm but cannot,
      // they will be added to mage again and again - without any way around.
      /*Right(
        entries.map { sellerDataChanged =>
          ImportConfirmation(
            sellerDataChanged.collectionId,
            success = false, added = false,
            info = "needToRemoveFromSnapcardster"
          )
        }
      )*/

    } else {
      Right(getConfirmationRaw(mkm.responseContent, "deleted", entries, added = false))
    }
  }

  def addToMkmStock(entries: Seq[SellerDataChanged], mkm: M11DedicatedApp): String = {
    if (entries.isEmpty)
      return ""

    val body = // NO SPACE at beginning, this is important
      s"""<?xml version="1.0" encoding="utf-8"?>
      <request>
      ${
        entries.flatMap { entry =>
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
          if (csvLine.length >= 14) {
            val prodId = csvLine(1)
            //val engName = csvLine(2)
            //val localName = csvLine(3)
            //val exp = csvLine(4)
            //val expName = csvLine(5)
            val price = csvLine(6)
            val language = csvLine(7)
            val condition = csvLine(8)
            val foil = isTrue(csvLine(9))
            val signed = isTrue(csvLine(10))
            //val playset = isTrue(csvLine(11))
            val altered = isTrue(csvLine(12))
            val comment = csvLine(13)
            //val amount = csvLine(14)
            //val onSale = csvLine(15)

            // Example:
            // "idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale"
            // "353185336";"294560";"Quicksmith Rebel";"Quicksmith Rebel";"AER";"Aether Revolt";"333.00";"1";"NM";"";"";"";"";"";"1";"1"

            // For post, all information must be present as well as count=1
            Some(
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
            """)
          } else {
            None
          }
        }.mkString("")
      }
      </request>
      """
    val hasOutput = false
    if (!mkmReqTimoutable(mkmStockEndpoint, "POST", (url, method) =>
      mkm.request(url, method, body, "application/xml", hasOutput))) {
      handleEx(mkm.lastError, entries)
      getErrorString(mkm)
    } else {
      getConfirmation(mkm.responseContent, "inserted", entries, added = true)
    }
  }

  def mkmReqTimoutable(endpoint: String, method: String, mkmRequestCall: (String, String) => Boolean): Boolean = {
    mkmCallsSoFar.setValue(mkmCallsSoFar.getValue + 1)
    val timeoutMs = Config.getTimeout
    TimeoutWatcher(timeoutMs, () => mkmRequestCall(endpoint, method)).run.getOrElse(
      sys.error("Timeout: " + method + " " + endpoint + " did not complete within " + timeoutMs + "ms")
    )
  }

  def isTrue(x: String): Boolean = {
    x.toLowerCase.contains("x") || x == "1" || x.toLowerCase == "true"
  }

  def xmlList(list: NodeList): Seq[Node] = {
    0.until(list.getLength).map(x => list.item(x)).toSeq
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
    doc.getDocumentElement.normalize
    doc
  }

  def getErrorString(mkm: M11DedicatedApp): String = {
    s"""{"error": s"Returned HTTP code ${mkm.responseCode}, exception: ${errorText(mkm.lastError)}"}"""
  }

  def getConfirmation(xml: String, tagName: String, entriesInRequest: Seq[SellerDataChanged], added: Boolean): String = {
    val confirmations = getConfirmationRaw(xml, tagName, entriesInRequest, added)
    /*if (Config.isVerbose) {
      println("confirmation xml: " + xml + "=>" + confirmations)
    }*/
    new Gson().toJson(confirmations)
  }

  def getConfirmationRaw(xml: String, tagName: String, entriesInRequest: Seq[SellerDataChanged], added: Boolean): Array[ImportConfirmation] = {
    val ex = nativeProvider.saveToFile(
      File.separatorChar + "logs" + File.separatorChar + "xml-" + tagName + "-" + System.currentTimeMillis + ".xml", xml, null
    )
    if (ex != null) {
      println("Failed writing info xml: " + ex)
    }

    val tags = try {
      xmlList(getXml(xml).getElementsByTagName(tagName))
    } catch {
      case x: Exception =>
        handleEx(x, "performing getXml from " + xml)
        Nil
    }

    val mkmConfirms: Seq[MkmConfirm] = tags.map { x =>
      val xs = xmlList(x.getChildNodes)
      val successful = java.lang.Boolean.parseBoolean(xs.find(_.getNodeName == "success").get.getTextContent)
      val message = xs.find(_.getNodeName == "message").map(_.getTextContent)
      val value = xs.find(_.getNodeName == "idArticle")
      // val englishName = xs.find(_.getNodeName == "engName").map(_.getTextContent)

      val ch = value.toList.flatMap(x => xmlList(x.getChildNodes))
      val idArticle =
        if (ch.size == 1 && ch.head.getNodeType == Node.TEXT_NODE) {
          ch.head.getTextContent.toLong
        } else {
          ch.find(_.getNodeName == "idArticle").map(_.getTextContent.toLong).getOrElse(-1L)
        }
      val count =
        ch.find(_.getNodeName == "count").map(_.getTextContent.toInt).getOrElse(0)


      if (message.nonEmpty) {
        println("getConfirmationRaw " + tagName.toUpperCase + " had message for " + idArticle + ": " + message.get + ", successful: " + successful)
      }

      MkmConfirm(
        externalId = idArticle,
        count = count,
        successful = successful,
        message = message.getOrElse("")
      )
    }

    val resultItems = entriesInRequest.map { entry =>
      val x = mkmConfirms.find(_.externalId == entry.externalId)

      val success = x.exists(_.successful) // if (!added) false else
      // we need succes = false for deletions (in an older version)
      ImportConfirmation(entry.collectionId, success, added,
        "" //  if (!added) "needToRemoveFromSnapcardster" else ""
      )
    }

    resultItems.toArray
  }

  def getOversizedCardNames: Array[String] = {
    val apiQuery =
      "https://api.scryfall.com/cards/search?q=is%3Aoversized+t%3ALegend+include%3Aextras"
    val source = Source.fromURL(apiQuery)
    val res = source.mkString
    source.close()
    val obj: ScryfallList = new Gson().fromJson(res, classOf[ScryfallList])
    //      System.out.println(obj.toString)
    val result = obj.data
    println("getOversizedCardNames (scryfall): " + result.length)
    result.map(x => x.name)

    /*val q = "https://scryfall.com/search?q=is%3Aoversized+t%3Alegend&unique=cards&as=checklist"
    val source = Source.fromURL(q)
    val html = source.mkString
    source.close()
    val x =
      html.split("\n").filter(_.contains("<td class=\"ellipsis\"><a href=\""))
    val names = x.map(x => getXml(x).getFirstChild.getFirstChild.getTextContent)
    names*/
  }
}
