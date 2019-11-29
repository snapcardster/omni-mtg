package omnimtg

import java.io._
import java.awt.Desktop
import java.net.URI
import java.nio.file.{Path, Paths}
import java.util.{Base64, Properties}

import javax.xml.bind.DatatypeConverter
import omnimtg.Interfaces._

class DesktopFunctionProvider() extends NativeFunctionProvider {
  private val configPath: Path = Paths.get("secret.properties")

  override def openLink(url: String): Unit = {
    val desktop = if (Desktop.isDesktopSupported) Desktop.getDesktop else null
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
      try {
        desktop.browse(new URI(url))
      } catch {
        case e: Throwable => e.printStackTrace
      }
    }
  }

  def updateProp(str: String, property: StringProperty, prop: Properties): Unit = {
    val value = prop.getProperty(str)
    if (value != null && value != "") {
      property.setValue(String.valueOf(value))
    }
  }

  def updateProp(str: String, property: DoubleProperty, prop: Properties): Unit = {
    val value = prop.getProperty(str)
    if (value != null && value != "") {
      property.setValue(java.lang.Double.parseDouble(String.valueOf(value)))
    }
  }

  def updateProperties(str: String, property: StringProperty, prop: Properties): Unit = {
    prop.setProperty(str, String.valueOf(property.getValue))
  }

  def updateProperties(str: String, property: DoubleProperty, prop: Properties): Unit = {
    prop.setProperty(str, property.getValue.toString)
  }

  def updateProperties[T](str: String, value: List[String], prop: Properties): Unit = {
    prop.setProperty(str, value.mkString("|"))
  }

  override def save(prop: Properties, rawController: Any, nativeBase: Object): Throwable = {
    val controller: MainController = rawController.asInstanceOf[MainController]

    updateProperties("mkmApp", controller.mkmAppToken, prop)
    updateProperties("mkmAppSecret", controller.mkmAppSecret, prop)
    updateProperties("mkmAccessToken", controller.mkmAccessToken, prop)
    updateProperties("mkmAccessTokenSecret", controller.mkmAccessTokenSecret, prop)
    updateProperties("snapUser", controller.snapUser, prop)
    updateProperties("snapToken", controller.snapToken, prop)
    updateProperties("bidPriceMultiplier", controller.bidPriceMultiplier, prop)
    updateProperties("minBidPrice", controller.minBidPrice, prop)
    updateProperties("maxBidPrice", controller.maxBidPrice, prop)
    updateProperties("askPriceMultiplier", controller.askPriceMultiplier, prop)

    updateProperties("bidFoils", controller.bidFoils.map(_.toString), prop)
    updateProperties("bidConditions", controller.bidConditions.map(_.toString), prop)
    updateProperties("bidLanguages", controller.bidLanguages.map(_.toString), prop)

    var str: OutputStream = new ByteArrayOutputStream()
    try {
      str = new FileOutputStream(configPath.toFile)
      prop.store(str, null)
    } catch {
      case e: Exception => return e
    } finally {
      str.close()
    }
    null
  }

  def readList[T](prop: Properties, key: String, f: String => T): List[T] = {
    val value = prop.getProperty(key)
    if (value != null && value != "") {
      value.split("""|""").map(f).toList
    } else {
      Nil
    }
  }

  override def readProperties(prop: Properties, mainController: Object, nativeBase: Object): Throwable = {
    val controller = mainController.asInstanceOf[MainController]

    if (!configPath.toFile.exists) {
      configPath.toFile.createNewFile
    }

    var str: InputStream = new ByteArrayInputStream(Array(42))
    try {
      str = new FileInputStream(configPath.toFile)
      prop.load(str)

      updateProp("mkmApp", controller.mkmAppToken, prop)
      updateProp("mkmAppSecret", controller.mkmAppSecret, prop)
      updateProp("mkmAccessToken", controller.mkmAccessToken, prop)
      updateProp("mkmAccessTokenSecret", controller.mkmAccessTokenSecret, prop)
      updateProp("snapUser", controller.snapUser, prop)
      updateProp("snapToken", controller.snapToken, prop)
      updateProp("bidPriceMultiplier", controller.bidPriceMultiplier, prop)
      updateProp("minBidPrice", controller.minBidPrice, prop)
      updateProp("maxBidPrice", controller.maxBidPrice, prop)
      updateProp("askPriceMultiplier", controller.askPriceMultiplier, prop)

      controller.bidFoils = readList(prop, "bidFoils", str => str.toBoolean)
      controller.bidLanguages = readList(prop, "bidLanguages", str => str.toInt)
      controller.bidConditions = readList(prop, "bidConditions", str => str.toInt)

      if (prop.getProperty("mkmAppToken") != null) {
        controller.output.setValue("Stored authentication information restored")
      }
    } catch {
      case e: Exception => e
    } finally {
      str.close()
    }
    null
  }

  override def decodeBase64(str: String): Array[Byte] = {
    Base64.getDecoder.decode(str)
  }

  override def saveToFile(path: String, contents: String, nativeBase: scala.Any): Throwable = {
    var writer: PrintWriter = null
    try {
      writer = new PrintWriter(path)
      writer.write(contents)
    } catch {
      case e: Exception => return e
    } finally {
      if (writer != null)
        writer.close()
    }
    null
  }

  override def encodeBase64ToString(digest: Array[Byte]): String = {
    DatatypeConverter.printBase64Binary(digest)
  }

  override def println(x: Any): Unit = {
    Predef.println(x)
  }
}
