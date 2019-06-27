package omnimtg

import javafx.application._

object Main {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[MainGUI], args: _*)
  }
}