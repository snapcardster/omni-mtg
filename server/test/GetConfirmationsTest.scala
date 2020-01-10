import java.io.{File, FileReader}
import java.util.Properties

import omnimtg._
import org.junit.{Assert, Test}

// @RunWith(BlockJUnit4ClassRunner.getClass)
class GetConfirmationsTest {
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

    val dummy =
      CsvFormat(
        0, "", "", false, Condition("", "", 0, ""), Language("", "", 0),
        "",
        0, "", false, false, 0.0, 0, 0
      )

    app.setDebug(true)
    Config.setVerbose(true)

    /*
      val csv = m.loadMkmStock(app)
      println("csv:\n" + csv.mkString("\n"))


      val asd = m.deleteFromMkmStock(
        List(585844936, 585841151, 85840741)
          .zipWithIndex.map { case (x, i) =>
          SellerDataChanged(
            `type` = "",
            externalId = x,
            collectionId = i,
            info = dummy
          )
        })

      println(asd)
    */

    val csvv =
      """
"idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale";"collectorNumber"
"585841106";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"1";"GD";"";"";"";"";"goody";"1";"1";""
"585840616";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.04";"1";"PO";"";"";"";"";"poor";"1";"1";""
"585844936";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.06";"1";"MT";"";"";"";"";"min";"1";"1";""
"585841151";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"1";"EX";"";"";"";"";"ec";"1";"1";""
"585840741";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.04";"1";"PL";"";"";"";"";"paly";"1";"1";""
"585841236";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"10";"NM";"";"";"";"";"";"1";"1";""
"585841186";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"1";"NM";"";"";"";"";"";"1";"1";""
"585840846";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.04";"1";"LP";"";"";"";"";"lightly pl";"1";"1";""
"583700611";"366171";"Liliana of the Veil";"Liliana of the Veil";"";"Ultimate Masters";"100.00";"1";"NM";"";"";"";"";"";"1";"1";""
"584662401";"372254";"Casualties of War";"Casualties of War";"";"War of the Spark";"2.00";"1";"NM";"";"";"";"";"";"1";"1";"187"
"584663296";"372265";"Huatli, the Sun's Heart";"Huatli, the Sun's Heart";"";"War of the Spark";"1.00";"1";"EX";"X";"";"";"";"";"1";"1";"230"
""".trim

    val xmlListOfDeletes =
    // m.xmlList(m.getXml(
      """<response>
        <deleted>
          <idArticle>585844936</idArticle> <count>1</count> <success>true</success>
        </deleted>
        <deleted>
          <idArticle>585841151</idArticle> <count>2</count> <success>true</success>
        </deleted>
        <deleted>
          <idArticle>85840741</idArticle> <count>1</count> <success>false</success>
          <message>Du bist nicht autorisiert um diese Aktion auszuführen</message>
        </deleted>

        <deleted>
          <idArticle>584662401</idArticle> <count>1</count> <success>false</success>
          <message>Ungültiger Artikel</message>
        </deleted>

      </response>""".trim //).getChildNodes)

    val changeItems = List(
      585844936 -> 1001L,
      585841151 -> 1002L, 585841151 -> 1003L, // qty 2 example
      85840741 -> 1004L,
      584662401 -> 1005L)
      .flatMap { case (idArt, collId) =>
        List(SellerDataChanged(
          `type` = "removed",
          externalId = idArt,
          collectionId = collId,
          info = dummy
        ))
      }

    val res =
      m.getConfirmationRaw(xmlListOfDeletes,
        tagName = "deleted", changeItems, added = false
      )

    Assert.assertArrayEquals(Array(
      ImportConfirmation(1001, true, false, ""),
      ImportConfirmation(1002, true, false, ""),
      ImportConfirmation(1003, true, false, ""),
      ImportConfirmation(1004, false, false, "needToRemoveFromSnapcardster"),
      ImportConfirmation(1005, false, false, "needToRemoveFromSnapcardster")
    ).map(x => x: AnyRef), res.map(x => x: AnyRef))

    println(res.mkString("\n™"))
  }
}
