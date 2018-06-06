package example

import java.awt.Desktop
import java.net.URI

class MainController {
  var mkmApp: String = ""
  var mkmAppToken: String = ""
  var mkmAppSecret: String = ""
  var mkmAppTokenSecret: String = ""

  def x() = {

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

}
