import java.io.{BufferedReader, ByteArrayInputStream, File, FileReader, InputStreamReader}
import java.net.{HttpURLConnection, URL}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.{io, util}
import java.util.Properties
import java.util.zip.GZIPInputStream

import com.google.gson.{Gson, GsonBuilder}
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import org.w3c.dom.{Document, Node, NodeList}

import scala.io.Source
import omnimtg._

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

    val ambig = m.getAmbigousProductIds(app)
    m.ambiguousProductIdLookup = ambig

    val csv: String = m
      .getCsvWithProductInfo(app)
      .map(_.mkString("\n"))
      .getOrElse("")
    //ambig.get.mkString("\n")


    val value = ambig.get.mkString("\n")
    // println("RES len: " + ambig.get.size + "\n" + value)
    val path1 = Paths.get("ambiguous.txt")
    Files.write(path1, value.getBytes, List(StandardOpenOption.CREATE): _*)
    println(path1)

    val path2 = Paths.get("karsten.csv")
    Files.write(path2, csv.getBytes, List(StandardOpenOption.CREATE): _*)
    println(path2)


    //    m.ambiguousProductIdLookup = m.getAmbigousProductIds(app)
    //val csvWithCol = m.loadMkmStock(app)
    //println("csvWithCol:" + csvWithCol)

  }
}
