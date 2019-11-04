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

    val doc = getXml(app.responseContent)
    val response = doc.getChildNodes.item(0)
    val xml = xmlList(response.getChildNodes)
    val idArticleToCollectorNumber =
      xml.flatMap { x =>
        val nodes = xmlList(x.getChildNodes)
        val subNodes =
          nodes.filter(x => x.getNodeName == "idArticle" || x.getNodeName == "product")

        subNodes.find(_.getNodeName == "idArticle").map(_.getTextContent).flatMap { id =>
          subNodes.find(_.getNodeName == "product").flatMap(x => xmlList(x.getChildNodes).find(_.getNodeName == "nr")
            .map(_.getTextContent)).map(collectorNumber =>
            id -> collectorNumber
          )
        }
      }.toMap

    println(idArticleToCollectorNumber.mkString("\n"))

    //println("ERR:" + app.lastError)

    app.request(mkmStockFileEndpoint, "GET", null, null, true)
    val csv = loadMkmCsvLines(app.responseContent)
    val csvWithCol = csv.map { line =>
      val index = line.indexOf("\";\"")
      val idArt = line.substring(1, index)
      if (idArt == "idArticle")
        line + ";\"Collector Number\""
      else
        line + idArticleToCollectorNumber.get(idArt).map(x => ";\"" + x + "\"").getOrElse("")
    }
    println(csvWithCol.mkString("\n"))
  }

  def loadMkmCsvLines(responseContent: String): Array[String] = {
    val builder = new GsonBuilder
    val obj = new Gson().fromJson(responseContent, classOf[MKMSomething])
    //      System.out.println(obj.toString)
    val result = obj.stock

    // get string content from base64'd gzip
    val arr: Array[Byte] = new DesktopFunctionProvider().decodeBase64(result)
    val asd: ByteArrayInputStream = new ByteArrayInputStream(arr)
    val gz: GZIPInputStream = new GZIPInputStream(asd)
    val rd: BufferedReader = new BufferedReader(new InputStreamReader(gz))
    val content: util.List[String] = new util.ArrayList[String]

    var abort = false
    while (!abort) {
      val line = rd.readLine
      if (line == null) {
        abort = true
      } else {
        content.add(line)
      }
    }
    content.toArray(new Array[String](content.size))
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
