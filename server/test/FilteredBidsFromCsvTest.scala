import java.io.{File, FileReader}
import java.util.Properties

import omnimtg.{DesktopFunctionProvider, JavaFXPropertyFactory, MainController, ParseMkm}
import org.junit.{Assert, Test}

// @RunWith(BlockJUnit4ClassRunner.getClass)
class FilteredBidsFromCsvTest {
  def main(args: Array[String]): Unit = {}

  @Test
  def test(): Unit = {

    val m = new MainController(JavaFXPropertyFactory, new DesktopFunctionProvider)

    var res = ""
    var expect = ""

    m.bidPriceMultiplier.setValue(0.5)
    m.minBidPrice.setValue(1.0)
    m.maxBidPrice.setValue(111.0)
    m.bidConditions = ParseMkm.allConditionsData.map(_.id).toList
    m.bidLanguages = ParseMkm.allLanguagesData.map(_.id).toList
    m.bidFoils = ParseMkm.allFoilsData.map(_.value).toList
    println("asdt")

    res = m.filterBids(
      Array(
        "\"idArticle\";\"idProduct\";\"English Name\";\"Local Name\";\"Exp.\";\"Exp. Name\";\"Price\";\"Language\";\"Condition\";\"Foil?\";\"Signed?\";\"Playset?\";\"Altered?\";\"Comments\";\"Amount\";\"onSale\";\"collectorNumber\"",
        "\"583700611\";\"366171\";\"Liliana of the Veil\";\"Liliana of the Veil\";\"\";\"Ultimate Masters\";\"100.00\";\"1\";\"NM\";\"\";\"\";\"\";\"\";\"\";\"1\";\"1\";\"\""
      )
    ).mkString("\n")
    expect = Array(
      "\"idArticle\";\"idProduct\";\"English Name\";\"Local Name\";\"Exp.\";\"Exp. Name\";\"Price\";\"Language\";\"Condition\";\"Foil?\";\"Signed?\";\"Playset?\";\"Altered?\";\"Comments\";\"Amount\";\"onSale\";\"collectorNumber\"",
      "\"583700611\";\"366171\";\"Liliana of the Veil\";\"Liliana of the Veil\";\"\";\"Ultimate Masters\";\"50.0\";\"1\";\"NM\";\"\";\"\";\"\";\"\";\"\";\"1\";\"1\";\"\""
    ).mkString("\n")

    println("Test\n" + res + "\nshould be\n" + expect)


    Assert.assertEquals(expect, res)

    // filter out

    m.bidPriceMultiplier.setValue(1.0)
    m.minBidPrice.setValue(1.0)
    m.maxBidPrice.setValue(91.0)
    m.bidConditions = ParseMkm.allConditionsData.map(_.id).toList
    m.bidLanguages = ParseMkm.allLanguagesData.map(_.id).toList
    m.bidFoils = ParseMkm.allFoilsData.map(_.value).toList

    res = m.filterBids(
      Array(
        "\"idArticle\";\"idProduct\";\"English Name\";\"Local Name\";\"Exp.\";\"Exp. Name\";\"Price\";\"Language\";\"Condition\";\"Foil?\";\"Signed?\";\"Playset?\";\"Altered?\";\"Comments\";\"Amount\";\"onSale\";\"collectorNumber\"",
        "\"583700611\";\"366171\";\"Liliana of the Veil\";\"Liliana of the Veil\";\"\";\"Ultimate Masters\";\"100.00\";\"1\";\"NM\";\"\";\"\";\"\";\"\";\"\";\"1\";\"1\";\"\""
      )
    ).mkString("\n")
    expect = Array(
      "\"idArticle\";\"idProduct\";\"English Name\";\"Local Name\";\"Exp.\";\"Exp. Name\";\"Price\";\"Language\";\"Condition\";\"Foil?\";\"Signed?\";\"Playset?\";\"Altered?\";\"Comments\";\"Amount\";\"onSale\";\"collectorNumber\""
    ).mkString("\n")

    println("Test\n" + res + "\nshould be\n" + expect)


    Assert.assertEquals(expect, res)

    // filter out

    m.bidPriceMultiplier.setValue(1.0)
    m.minBidPrice.setValue(1.0)
    m.maxBidPrice.setValue(111.0)
    m.bidConditions = ParseMkm.allConditionsData.map(_.id).toList
    m.bidLanguages = ParseMkm.allLanguagesData.filterNot(x => x.twoLetterCode == "EN").map(_.id).toList
    m.bidFoils = ParseMkm.allFoilsData.map(_.value).toList

    res = m.filterBids(
      Array(
        "\"idArticle\";\"idProduct\";\"English Name\";\"Local Name\";\"Exp.\";\"Exp. Name\";\"Price\";\"Language\";\"Condition\";\"Foil?\";\"Signed?\";\"Playset?\";\"Altered?\";\"Comments\";\"Amount\";\"onSale\";\"collectorNumber\"",
        "\"583700611\";\"366171\";\"Liliana of the Veil\";\"Liliana of the Veil\";\"\";\"Ultimate Masters\";\"100.00\";\"1\";\"NM\";\"\";\"\";\"\";\"\";\"\";\"1\";\"1\";\"\""
      )
    ).mkString("\n")
    expect = Array(
      "\"idArticle\";\"idProduct\";\"English Name\";\"Local Name\";\"Exp.\";\"Exp. Name\";\"Price\";\"Language\";\"Condition\";\"Foil?\";\"Signed?\";\"Playset?\";\"Altered?\";\"Comments\";\"Amount\";\"onSale\";\"collectorNumber\""
    ).mkString("\n")

    println("Test\n" + res + "\nshould be\n" + expect)


    Assert.assertEquals(expect, res)

    // filter out

    m.bidPriceMultiplier.setValue(1.0)
    m.minBidPrice.setValue(1.0)
    m.maxBidPrice.setValue(111.0)
    m.bidConditions = ParseMkm.allConditionsData.map(_.id).toList
    m.bidLanguages = ParseMkm.allLanguagesData.filterNot(x => x.twoLetterCode == "EN").map(_.id).toList
    m.bidFoils = ParseMkm.allFoilsData.map(_.value).toList

    res = m.filterBids(
      Array(
        """"idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale";"collectorNumber"""",
        """"583700611";"366171";"Liliana of the Veil";"Liliana of the Veil";"";"Ultimate Masters";"100.00";"1";"NM";"";"";"";"";"";"1";"1";""""",
        """"584662401";"372254";"Casualties of War";"Casualties of War";"";"War of the Spark";"3.00";"1";"NM";"";"";"";"";"";"1";"1";"187"""",
        """"584663296";"372265";"Huatli, the Sun's Heart";"Huatli, the Sun's Heart";"";"War of the Spark";"1.00";"2";"GD";"X";"";"";"";"";"1";"1";"230""""
      )
    ).mkString("\n")
    expect = Array(
      """"idArticle";"idProduct";"English Name";"Local Name";"Exp.";"Exp. Name";"Price";"Language";"Condition";"Foil?";"Signed?";"Playset?";"Altered?";"Comments";"Amount";"onSale";"collectorNumber"""",
      """"584663296";"372265";"Huatli, the Sun's Heart";"Huatli, the Sun's Heart";"";"War of the Spark";"1.0";"2";"GD";"X";"";"";"";"";"1";"1";"230""""
    ).mkString("\n")

    println("Test\n" + res + "\nshould be\n" + expect)

    Assert.assertEquals(expect, res)

  }
}
