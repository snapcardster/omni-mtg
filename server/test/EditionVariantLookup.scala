import java.io.{File, FileReader}
import java.util.Properties
import omnimtg._
import org.junit.Test

// @RunWith(BlockJUnit4ClassRunner.getClass)
class EditionVariantLookup {
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

    Config.setVerbose(true)
    m.ambiguousProductIdLookup = m.getAmbigousProductIds(app)
    val csvWithCol = m.getCsvWithProductInfo(app)
    println(csvWithCol.get.mkString("\n"))

    /*
    BEFORE
    "idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale";"collectorNumber"
    "618141294";"7437";"High Tide";"High Tide";"";"Fallen Empires";"31.00";"1";"NM";"";"";"";"";"";"1";"1";""
    "618141209";"7436";"High Tide";"High Tide";"";"Fallen Empires";"33.00";"1";"NM";"";"";"";"";"";"1";"1";""
    "585844936";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.06";"1";"MT";"";"";"";"";"min";"1";"1";""
    "585841151";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"1";"EX";"";"";"";"";"ec";"1";"1";""
    "585840741";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.04";"1";"PL";"";"";"";"";"paly";"1";"1";""
    "585841236";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"10";"NM";"";"";"";"";"";"1";"1";""
    "585841186";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"1";"NM";"";"";"";"";"";"1";"1";""
    "585840846";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.04";"1";"LP";"";"";"";"";"lightly pl";"1";"1";""
    "585841106";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.05";"1";"GD";"";"";"";"";"goody";"1";"1";""
    "585840616";"374406";"Snow-Covered Plains";"Snow-Covered Plains";"";"Modern Horizons";"0.04";"1";"PO";"";"";"";"";"poor";"1";"1";""
    "583700611";"366171";"Liliana of the Veil";"Liliana of the Veil";"";"Ultimate Masters";"100.00";"1";"NM";"";"";"";"";"";"1";"1";""
    "584662401";"372254";"Casualties of War";"Casualties of War";"";"War of the Spark";"2.00";"1";"NM";"";"";"";"";"";"1";"1";"187"
    "584663296";"372265";"Huatli, the Sun's Heart";"Huatli, the Sun's Heart";"";"War of the Spark";"1.00";"1";"EX";"X";"";"";"";"";"1";"1";"230"

    AFTER

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

     */

    // m.calcLookup(app, Set(7437, 7437, 7436))
    // fem combat medic   7534))
    /*
<?xml version="1.0" encoding="utf-8"?>
<response>
  <product>
    <idProduct>7534</idProduct>
    <idMetaproduct>1085</idMetaproduct>
    <countReprints>9</countReprints>
    <enName>Combat Medic (Version 2)</enName>
    <locName>Combat Medic (Version 2)</locName>
    <localization>
      <name>Combat Medic</name>
      <idLanguage>1</idLanguage>
      <languageName>English</languageName>
    </localization>
    <localization>
      <name>Combat Medic</name>
      <idLanguage>2</idLanguage>
      <languageName>French</languageName>
    </localization>
    <localization>
      <name>Combat Medic</name>
      <idLanguage>3</idLanguage>
      <languageName>German</languageName>
    </localization>
    <localization>
      <name>Combat Medic</name>
      <idLanguage>4</idLanguage>
      <languageName>Spanish</languageName>
    </localization>
    <localization>
      <name>Combat Medic</name>
      <idLanguage>5</idLanguage>
      <languageName>Italian</languageName>
    </localization>
    <website>/en/Magic/Products/Singles/Fallen-Empires/Combat-Medic-Version-2</website>
    <image>/srv/home/www/img/items/1/FEM/7534.jpg</image>
    <gameName>Magic the Gathering</gameName>
    <categoryName>Magic Single</categoryName>
    <idGame>1</idGame>
    <number/>
    <rarity>Common</rarity>
    <expansion>
      <idExpansion>9</idExpansion>
      <enName>Fallen Empires</enName>
      <expansionIcon>8</expansionIcon>
    </expansion>
    <priceGuide>
      <SELL>0.05</SELL>
      <LOW>0.01</LOW>
      <LOWEX>0.02</LOWEX>
      <LOWFOIL>0</LOWFOIL>
      <AVG>0.31</AVG>
      <TREND>0.02</TREND>
      <TRENDFOIL>0</TRENDFOIL>
    </priceGuide>
    <reprint>
      <idProduct>16629</idProduct>
      <expansion>Anthologies</expansion>
      <expIcon>72</expIcon>
    </reprint>
    <reprint>
      <idProduct>7536</idProduct>
      <expansion>Fallen Empires</expansion>
      <expIcon>8</expIcon>
    </reprint>
    <reprint>
      <idProduct>7535</idProduct>
      <expansion>Fallen Empires</expansion>
      <expIcon>8</expIcon>
    </reprint>
    <reprint>
      <idProduct>7533</idProduct>
      <expansion>Fallen Empires</expansion>
      <expIcon>8</expIcon>
    </reprint>
    <reprint>
      <idProduct>279506</idProduct>
      <expansion>Fallen Empires: Wyvern Misprints</expansion>
      <expIcon>355</expIcon>
    </reprint>
    <reprint>
      <idProduct>279438</idProduct>
      <expansion>Fallen Empires: Wyvern Misprints</expansion>
      <expIcon>355</expIcon>
    </reprint>
    <reprint>
      <idProduct>279397</idProduct>
      <expansion>Fallen Empires: Wyvern Misprints</expansion>
      <expIcon>355</expIcon>
    </reprint>
    <reprint>
      <idProduct>279360</idProduct>
      <expansion>Fallen Empires: Wyvern Misprints</expansion>
      <expIcon>355</expIcon>
    </reprint>
    <countArticles>712</countArticles>
    <countFoils>0</countFoils>
    <links>
      <rel>self</rel>
      <href>/products/7534</href>
      <method>GET</method>
    </links>
    <links>
      <rel>articles</rel>
      <href>/articles/7534</href>
      <method>GET</method>
    </links>
  </product>
</response>
     */

  }
}
