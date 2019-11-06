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

    /*val asd: Map[String, List[MKMProductEntry]] = Map(
      "Murderous Rider" ->
        List(MKMProductEntry("403769", "Murderous Rider", "2694", "Throne of Eldraine: Promos"),
          MKMProductEntry("403429", "Murderous Rider", "2694", "Throne of Eldraine: Promos"),
          MKMProductEntry("399984", "Murderous Rider", "2587", "Throne of Eldraine: Extras"),
          MKMProductEntry("399979", "Murderous Rider", "2582", "Throne of Eldraine")),

      "Smuggler's Copter" -> List(MKMProductEntry("292981", "Smuggler's Copter", "1726", "Kaladesh: Promos"),
        MKMProductEntry("292597", "Smuggler's Copter", "1717", "Kaladesh")
      )
    )
    val res = asd.filter(x => x._2.exists(y => x._2.exists(_.expansionName == y.expansionName + ": Extras")))
    println(res)

    return ()*/

    //val xml2 = m.getAmbigousProductIds(app)
    //    val xml2 = m.getCsvWithProductInfo(app).getOrElse("")
    //xml2.get.mkString("\n")
    //println("RES:" + xml2.get.mkString("\n") + ", len: " + xml2.get.size)

    m.ambiguousProductIdLookup = m.getAmbigousProductIds(app)
    val csvWithCol = m.getCsvWithProductInfo(app)
    println("csvWithCol:" + csvWithCol)

  }
}
