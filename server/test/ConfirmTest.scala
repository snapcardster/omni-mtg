import java.io.{File, FileReader}
import java.util.Properties

import omnimtg.{Condition, Config, CsvFormat, DesktopFunctionProvider, JavaFXPropertyFactory, Language, M11DedicatedApp, MainController, SellerDataChanged}
import org.junit.Test

// @RunWith(BlockJUnit4ClassRunner.getClass)
class ConfirmTest {
  def main(args: Array[String]): Unit = {}

  @Test
  def test(): Unit = {
    val properties = new Properties()
    val home = System.getProperty("user.home");
    val path = home + "/Downloads/secret.properties";
    val file = new File(path)
    assert(file.exists)
    properties.load(new FileReader(file))

    val (mkmApp, mkmAppSecret, mkmAccessToken, mkmAccessTokenSecret) =
      (properties.getProperty("mkmApp"),
        properties.getProperty("mkmAppSecret"),
        properties.getProperty("mkmAccessToken"),
        properties.getProperty("mkmAccessTokenSecret")
      )
    val m = new MainController(JavaFXPropertyFactory, new DesktopFunctionProvider)
    m.mkmAppToken.setValue(mkmApp)
    m.mkmAppSecret.setValue(mkmAppSecret)
    m.mkmAccessToken.setValue(mkmAccessToken)
    m.mkmAccessTokenSecret.setValue(mkmAccessTokenSecret)

    val app = new M11DedicatedApp(
      mkmApp, mkmAppSecret, mkmAccessToken, mkmAccessTokenSecret,
      new DesktopFunctionProvider
    )

    m.snapUser.setValue(properties.getProperty("snapUser"))
    m.snapToken.setValue(properties.getProperty("snapToken"))

    m.snapBaseUrl = "https://dev.snapcardster.com"

    app.setDebug(true)

    val id = 242476
    val collId = 1337L
    val addedItems = List(SellerDataChanged(m.ADDED, id, collId, CsvFormat(
      0, "", "", false, Condition("", "", 0, ""), Language("", "", 0),
      "\"572041706\";\"" + id + "\";\"Kozilek's Return\";\"Kozilek's Return\";\"\";\"Oath of the Gatewatch\";\"122.00\";\"1\";\"NM\";\"\";\"\";\"\";\"\";\"\";\"1\";\"1\";\"\"",
      0, "", false, false, 0.0, 0, 0
    )))

    Config.setVerbose(true)
    val res = m.addToMkmStock(addedItems, app)
    println(res)

    /*

Requesting POST https://api.cardmarket.com/ws/v2.0/stockTue Jan 21 10:46:04 CET 2020 > Requesting POST https://api.cardmarket.com/ws/v2.0/stock
 Response Code is 200

<?xml version="1.0" encoding="utf-8"?><response>  <inserted>    <success>true</success>    <idArticle>      <idArticle>633644399</idArticle>      <idProduct>242476</idProduct>      <language>        <idLanguage>1</idLanguage>        <languageName>English</languageName>      </language>      <comments></comments>      <price>122</price>      <count>1</count>      <inShoppingCart>false</inShoppingCart>      <product>        <idGame>1</idGame>        <enName>Energy Chamber</enName>        <locName>Energy Chamber</locName>        <image>/srv/home/www/img/items/1/DDF/242476.jpg</image>        <expansion>Duel Decks: Elspeth vs. Tezzeret</expansion>        <nr>64</nr>        <expIcon>164</expIcon>        <rarity>Uncommon</rarity>      </product>      <lastEdited>2020-01-21T10:46:05+0100</lastEdited>      <condition>NM</condition>      <isFoil>false</isFoil>      <isSigned>false</isSigned>      <isPlayset>false</isPlayset>      <isAltered>false</isAltered>    </idArticle>  </inserted></response>
[{"collectionId":0,"successful":false,"added":true,"info":""}]


*/

    // m.loadSnapChangedAndDeleteFromStock(new StringBuilder)

    //val csv = m.loadMkmStock(app)

    //val res = m.postToSnap(csv)

    // output.setValue(outputPrefix() + snapCsvEndpoint + "\n" + res)
    // println("res has a length of " + res.length)
    //val items = m.loadChangedFromSnap()
    //println(items)

    /*
    println(resAdd)

  val auth = properties.getProperty("snapUser") + "," + properties.getProperty("snapToken")
  val body = if (resAdd.isEmpty) "[]" else resAdd
  val res = m.snapConnector.call(m.snapChangedEndpoint, "POST", auth, body)

  println(res)
    */

  }
}
