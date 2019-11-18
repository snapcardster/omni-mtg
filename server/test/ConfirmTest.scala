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

    val app = new M11DedicatedApp(
      properties.getProperty("mkmApp"),
      properties.getProperty("mkmAppSecret"),
      properties.getProperty("mkmAccessToken"),
      properties.getProperty("mkmAccessTokenSecret"),
      new DesktopFunctionProvider
    )

    app.setDebug(true)
    val m = new MainController(JavaFXPropertyFactory, new DesktopFunctionProvider)


    val id = 242476
    val addedItems = List(SellerDataChanged(m.REMOVED, 0, 0, CsvFormat(
      0, "", "", false, Condition("", "", 0, ""), Language("", "", 0),
      "\"572041706\";\"" + id + "\";\"Kozilek's Return\";\"Kozilek's Return\";\"\";\"Oath of the Gatewatch\";\"122.00\";\"1\";\"NM\";\"\";\"\";\"\";\"\";\"\";\"1\";\"1\";\"\"",
      0, "", false, false, 0.0, 0, 0
    )))

    Config.setVerbose(true)

    val resAdd = m.addToMkmStock(addedItems, app)

    println(resAdd)

    val auth = properties.getProperty("snapUser") + "," + properties.getProperty("snapToken")
    val body = if (resAdd.isEmpty) "[]" else resAdd
    val res = m.snapConnector.call(m.snapChangedEndpoint, "POST", auth, body)

    println(res)


  }
}
