import java.io.{BufferedReader, ByteArrayInputStream, File, FileReader, InputStreamReader}
import java.net.{HttpURLConnection, URL}
import java.util
import java.util.Properties
import java.util.zip.GZIPInputStream

import com.google.gson.{Gson, GsonBuilder}
import javax.xml.parsers.DocumentBuilderFactory
import omnimtg.{DesktopFunctionProvider, _}
import org.junit.Test
import org.w3c.dom.{Document, Node, NodeList}

// @RunWith(BlockJUnit4ClassRunner.getClass)
class ProductLookup {
  def main(args: Array[String]): Unit = {}

  @Test
  def test(): Unit = {
    val properties = new Properties()
    val home = System.getProperty("user.home");
    val path = home + "/Downloads/secret.properties";
    val file = new File(path)
    assert(file.exists)
    properties.load(new FileReader(file))

    val app = new M11DedicatedApp(
      properties.getProperty("mkmApp"),
      properties.getProperty("mkmAppSecret"),
      properties.getProperty("mkmAccessToken"),
      properties.getProperty("mkmAccessTokenSecret"),
      new DesktopFunctionProvider
    )

    app.setDebug(true)
    val m = new MainController(JavaFXPropertyFactory, new DesktopFunctionProvider)

    val xml2 = m.getCsvWithProductInfo(app).getOrElse("")

    println("RES:" + xml2 + ", len: " + xml2.length)
  }
}
