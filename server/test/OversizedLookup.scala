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

import scala.collection.mutable

// @RunWith(BlockJUnit4ClassRunner.getClass)
class OversizedLookup {
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

    /**/
    // app.request(m.mkmBaseUrl + "/metaproductlist", "GET", null, null, true);


    /*app.request(m.mkmProductEndpoint + "/" + 293925, "GET", null, null, true);
    val xmlDOC = m.getXml(app.responseContent)
    val x = m.xmlList(xmlDOC.getFirstChild.getChildNodes)
    val y = x.find(x => x.getNodeName == "product")
    println(y)*/

/*
    app.request(m.mkmProductFileEndpoint, "GET", null, null, true);
    val productJson = app.responseContent
    //println(productJson)

    val obj = new Gson().fromJson(productJson, classOf[MKMProductsfile])
    //      System.out.println(obj.toString)
    val result = obj.productsfile
    //          0,     1,            2,         3,             4,            5            6
    //"idProduct","Name","Category ID","Category","Expansion ID","Metacard ID","Date Added"
    //"1","Altar's Light","1","Magic Single","45","129","2007-01-01 00:00:00"
    val strings = m.unzipB64StringAndGetLines(result)
    val lines = strings.zipWithIndex.flatMap { case (x, i) =>
      if (i == 0 || x.contains("Magic Single")) {
        /*val parts = x.split("\",\"")
        val enName = parts(1)
        val idExpansion = parts(4).toLong
        Some(MKMProductEntry(
          idProduct = parts(0).substring(1).toLong,
          enName = enName,
          idExpansion = idExpansion,
          expansionName = ""
        ))*/
        Some(x)
      }
      else {
        None
      }
    }
    println(lines.mkString("\n"))*/
  }
}
