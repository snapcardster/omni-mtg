package example

import java.awt.datatransfer.DataFlavor
import java.awt.{Desktop, Toolkit}
import java.io._
import java.net.URI
import java.nio.file.{Path, Paths}
import java.util.regex.{Matcher, Pattern}
import java.util.{Date, Properties}

import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}

class MainController {
  def insertFromClip(mode: String): Unit = {
    val data = String.valueOf(Toolkit.getDefaultToolkit.getSystemClipboard.getData(DataFlavor.stringFlavor))
    mode match {
      case "mkm" =>
        val p: Pattern = Pattern.compile(".*App token(.*)\\s+App secret(.*)\\s+Access token(.*)\\s+Access token secret(.*)\\s*.*?")
        val matcher: Matcher = p.matcher(data)
        if (matcher.find()) {
          mkmAppToken.setValue(matcher.group(1))
          mkmAppSecret.setValue(matcher.group(2))
          mkmAccessToken.setValue(matcher.group(3))
          mkmAccessTokenSecret.setValue(matcher.group(4))
        }
      case "snap" =>
        val p: Pattern = Pattern.compile(".*User(.*)\\s+Token(.*)\\s*.*?")
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
    } finally {
      str.close()
    }

    run()
  }

  val prop: Properties = new Properties
  val configPath: Path = Paths.get("secret.properties")
  var aborted: BooleanProperty = new SimpleBooleanProperty(false)
  var running: BooleanProperty = new SimpleBooleanProperty(false)

  val mkmAppToken: StringProperty = prop("mkmApp")
  val mkmAppSecret: StringProperty = prop("mkmAppSecret")
  val mkmAccessToken: StringProperty = prop("mkmAccessToken")
  val mkmAccessTokenSecret: StringProperty = prop("mkmAccessTokenSecret")
  val snapUser: StringProperty = prop("snapUser")
  val snapToken: StringProperty = prop("snapToken")

  val output: StringProperty = new SimpleStringProperty("Output...")
  val interval: IntegerProperty = new SimpleIntegerProperty(1)

  private def prop(name: String): SimpleStringProperty = {
    val stringProp = new SimpleStringProperty("")
    val l = new ChangeListener[Any] {
      def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
        prop.put(name, String.valueOf(newValue))
        // save here?
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
    } finally {
      str.close()
    }
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
      case e: Exception =>
        e.printStackTrace()
    }
  }

  def run(): Unit = {
    new Thread(() =>
      while (!aborted.getValue) try {
        if (running.getValue) {
          val str = "Tick, running: " + running.getValue + ": " + new Date()
          output.setValue(output.getValue + "\n" + str)
        }

        val sec = interval.getValue.intValue
        val ms = sec * 1000
        Thread.sleep(ms)
      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
    ).start()
  }

}
