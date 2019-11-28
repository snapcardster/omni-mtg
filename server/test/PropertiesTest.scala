import java.io.{File, FileReader}
import java.util.Properties

import omnimtg._
import org.junit.{Assert, Test}

// @RunWith(BlockJUnit4ClassRunner.getClass)
class PropertiesTest {
  def main(args: Array[String]): Unit = {}

  @Test
  def test(): Unit = {
    val properties = new Properties()
    properties.setProperty("a", "d")
  }
}
