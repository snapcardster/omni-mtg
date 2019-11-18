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

  override def save(prop: Properties, nativeBase: Object): Throwable = {
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

  def updateProp(str: String, property: StringProperty, prop: Properties): Unit = {
    val value = prop.get(str)
    if (value != null && value != "") {
      property.setValue(String.valueOf(value))
    }
  }

  override def readProperties(prop: Properties, controller: MainControllerInterface, nativeBase: Object): Throwable = {
    if (!configPath.toFile.exists) {
      configPath.toFile.createNewFile
    }

    var str: InputStream = new ByteArrayInputStream(Array(42))
    try {
      str = new FileInputStream(configPath.toFile)
      prop.load(str)

      updateProp("mkmApp", controller.getMkmAppToken, prop)
      updateProp("mkmAppSecret", controller.getMkmAppSecret, prop)
      updateProp("mkmAccessToken", controller.getMkmAccessToken, prop)
      updateProp("mkmAccessTokenSecret", controller.getMkmAccessTokenSecret, prop)
      updateProp("snapUser", controller.getSnapUser, prop)
      updateProp("snapToken", controller.getSnapToken, prop)

      if (prop.get("mkmAppToken") != null) {
        controller.getOutput.setValue("Stored authentication information restored")
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
