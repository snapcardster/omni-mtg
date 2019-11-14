import java.io.{File, FileReader}
import java.util.Properties

import omnimtg.{DesktopFunctionProvider, _}
import org.junit.Test

import scala.io.Source

// @RunWith(BlockJUnit4ClassRunner.getClass)
class ScryfallFetchOversized {
  def main(args: Array[String]): Unit = {}

  @Test
  def test(): Unit = {
    val m = new MainController(JavaFXPropertyFactory, new DesktopFunctionProvider)
    val names: Array[String] = m.getOversizedCardNames
    println(names.length + ":\n\n" + names.mkString("\n"))
  }
}
