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
    m.getMkmAppToken.setValue(mkmApp)
    m.getMkmAppSecret.setValue(mkmAppSecret)
    m.getMkmAccessToken.setValue(mkmAccessToken)
    m.getMkmAccessTokenSecret.setValue(mkmAccessTokenSecret)

    val app = new M11DedicatedApp(
      mkmApp, mkmAppSecret, mkmAccessToken, mkmAccessTokenSecret,
      new DesktopFunctionProvider
    )

    m.getSnapUser.setValue(properties.getProperty("snapUser"))
    m.getSnapToken.setValue(properties.getProperty("snapToken"))

    m.snapBaseUrl = "https://dev.snapcardster.com"

    app.setDebug(true)

    val id = 242476
    val addedItems = List(SellerDataChanged(m.REMOVED, 0, 0, CsvFormat(
      0, "", "", false, Condition("", "", 0, ""), Language("", "", 0),
      "\"572041706\";\"" + id + "\";\"Kozilek's Return\";\"Kozilek's Return\";\"\";\"Oath of the Gatewatch\";\"122.00\";\"1\";\"NM\";\"\";\"\";\"\";\"\";\"\";\"1\";\"1\";\"\"",
      0, "", false, false, 0.0, 0, 0
    )))

    Config.setVerbose(true)

    // m.loadSnapChangedAndDeleteFromStock(new StringBuilder)

    val csv = m.loadMkmStock(app)

    val res = m.postToSnap(csv)

    // output.setValue(outputPrefix() + snapCsvEndpoint + "\n" + res)
    println("res has a length of " + res.length)
    val items = m.getChangeItems(res)
    println(items.toList)
    /*
    println(resAdd)

  val auth = properties.getProperty("snapUser") + "," + properties.getProperty("snapToken")
  val body = if (resAdd.isEmpty) "[]" else resAdd
  val res = m.snapConnector.call(m.snapChangedEndpoint, "POST", auth, body)

  println(res)
    */

  }
}
