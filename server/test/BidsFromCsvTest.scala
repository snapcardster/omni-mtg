import java.io.{File, FileReader}
import java.util.Properties

import omnimtg.{Condition, Config, CsvFormat, DesktopFunctionProvider, JavaFXPropertyFactory, Language, M11DedicatedApp, MainController, ParseMkm, SellerDataChanged}
import org.junit.{Assert, Test}

// @RunWith(BlockJUnit4ClassRunner.getClass)
class BidsFromCsvTest {
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


    Config.setVerbose(true)

    // m.loadSnapChangedAndDeleteFromStock(new StringBuilder)

    val csv = m.loadMkmStock(app)

    println(csv.mkString("\n"))

    m.bidConditions = ParseMkm.allConditionsData.map(_.id).toList
    m.bidLanguages = ParseMkm.allLanguagesData.map(_.id).toList
    m.bidFoils = ParseMkm.allFoilsData.map(_.value).toList

    var csvFiltered = m.filterBids(csv).length
    Assert.assertEquals("all cards filtered out", 1, csvFiltered)

    var items = m.postToSnapBids(csv)
    Assert.assertEquals("", items)

    m.bidPriceMultiplier.setValue(0.5)
    m.minBidPrice.setValue(0.00)
    m.maxBidPrice.setValue(100.00)

    csvFiltered = m.filterBids(csv).length
    Assert.assertNotEquals(1, csvFiltered)

    items = m.postToSnapBids(csv)
    println("Res: " + items)
    Assert.assertNotEquals("", items)
    /*
    OK:
    https://dev.snapcardster.com/importer/sellerdata/bidsFromCsv/3 response code:200
    {"imported":[],"errors":[],"nonMain":[0,2,0],"deferred":false}
    */

  }
}
