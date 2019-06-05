package omnimtg

import omnimtg.Interfaces.PropertyFactory
import java.lang
import java.util.Properties

import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}

class Config {}

object Config {
  private var verbose = false

  def isVerbose: Boolean = verbose

  def setVerbose(x: Boolean): Unit = {
    verbose = x
  }
}