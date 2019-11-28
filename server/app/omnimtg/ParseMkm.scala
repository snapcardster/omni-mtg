package omnimtg

// copied from typescript
object ParseMkm {
  val undefined = "";

  case class Entry(
                    name: String, twoLetterCode: String = "",
                    country: String = "", usName: String = "",
                    id: Int,
                    value: Boolean = false,
                    mkmCode: String = "")

  val allLanguagesData = Array(
    Entry(name = "English", twoLetterCode = "EN", id = 1, country = "US"),
    Entry(name = "French", twoLetterCode = "FR", id = 2, country = undefined),
    Entry(name = "German", twoLetterCode = "DE", id = 3, country = undefined),
    Entry(name = "Spanish", twoLetterCode = "ES", id = 4, country = undefined),
    Entry(name = "Italian", twoLetterCode = "IT", id = 5, country = undefined),
    Entry(name = "Portuguese", twoLetterCode = "PO", id = 6, country = "PT"),
    Entry(name = "Russian", twoLetterCode = "RU", id = 7, country = undefined),
    Entry(name = "Chinese", twoLetterCode = "CN", id = 8, country = undefined),
    Entry(name = "Japanese", twoLetterCode = "JP", id = 9, country = undefined),
    Entry(name = "Korean", twoLetterCode = "KO", id = 10, country = "KR"),
    Entry(name = "Traditional Chinese", twoLetterCode = "TC", id = 11, country = "CN")
  );

  val allConditionsData = Array(
    Entry(name = "Damaged", twoLetterCode = "DM", id = 1, usName = "Damaged"),
    Entry(name = "Played", twoLetterCode = "PL", id = 2, usName = "Heavily Played"),
    Entry(name = "Good", twoLetterCode = "GD", id = 3, usName = "Moderately Played"),
    Entry(name = "Excellent", twoLetterCode = "EX", id = 4, usName = "Lightly Played"),
    Entry(name = "Near Mint", twoLetterCode = "NM", id = 5, usName = "Near Mint")
  );

  val allFoilsData = Array(
    Entry(name = "Non-Foil", id = 0, value = false, mkmCode = ""),
    Entry(name = "Foil", id = 1, value = true, mkmCode = "X")
  );

  def parseLanguage(str: String): Int = {
    val number = str.toInt
    allLanguagesData.find(_.id == number).get.id
  }

  def parseCondition(str: String): Int = {
    allConditionsData.find(_.twoLetterCode == str).get.id
  }

  def parseFoil(str: String): Boolean = {
    allFoilsData.find(_.mkmCode == str).get.value
  }
}