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

    val mkmBaseUrl: String = "https://api.cardmarket.com/ws/v2.0"
    val mkmStockEndpoint: String = mkmBaseUrl + "/stock"
    val mkmStockFileEndpoint: String = mkmBaseUrl + "/output.json/stock/file"

    val app = new M11DedicatedApp(
      properties.getProperty("mkmApp"),
      properties.getProperty("mkmAppSecret"),
      properties.getProperty("mkmAccessToken"),
      properties.getProperty("mkmAccessTokenSecret"),
      new DesktopFunctionProvider
    )
    app.setDebug(true)
    app.request(mkmStockEndpoint, "GET", null, null, true)

    println(app.responseContent)
    val xml = app.responseContent


    //println("ERR:" + app.lastError)

    app.request(mkmStockFileEndpoint, "GET", null, null, true)
    val m = new MainController(JavaFXPropertyFactory, new DesktopFunctionProvider)
    val jsonWithCsv = app.responseContent

    val csv = m.buildNewCsv(xml, jsonWithCsv)
    println(csv)
  }

  def xmlList(list: NodeList): List[Node] = {
    0.until(list.getLength).map(x => list.item(x)).toList
  }

  def getXml(xmlDoc: String): Document = {
    val dbFactory = DocumentBuilderFactory.newInstance
    //dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    //dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    //dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    //dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    //dbFactory.setXIncludeAware(false)
    dbFactory.setExpandEntityReferences(false)
    val dBuilder = dbFactory.newDocumentBuilder
    val doc = dBuilder.parse(new ByteArrayInputStream(xmlDoc.getBytes("UTF-8")))
    doc.getDocumentElement.normalize()
    doc
  }
}
