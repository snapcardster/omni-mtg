package omnimtg

import omnimtg.Interfaces.PropertyFactory
import java.lang
import java.util.Properties

import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}

class Config {}

object Config {
  private var verbose = false

  private var timeout = 30 * 60 * 1000 // 60 * 60 * 1000

  def getTimeout: Int = timeout

  def setTimeout(x: Int): Unit = {
    timeout = x
  }

  def isVerbose: Boolean = verbose

  def setVerbose(x: Boolean): Unit = {
    verbose = x
  }
}