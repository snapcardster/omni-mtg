package omnimtg

import javax.json.JsonObject

case class Condition(
                      longString: String,
                      shortString: String,
                      code: Int,
                      usString: String
                    )

object Condition {
  def parse(value: JsonObject): Condition = {
    Condition(
      value.getString("longString"),
      value.getString("shortString"),
      value.getInt("code"),
      value.getString("usString")
    )
  }
}

case class Language(
                     longString: String,
                     shortString: String,
                     code: Int
                   )

object Language {
  def parse(value: JsonObject): Language = {
    Language(
      value.getString("longString"),
      value.getString("shortString"),
      value.getInt("code")
    )
  }
}

case class CsvFormat(
                      qty: Int,
                      name: String,
                      editionCode: Option[String],
                      foil: Boolean,
                      condition: Condition,
                      language: Language,
                      board: String,
                      cardId: Option[Long],
                      editionName: Option[String],
                      signed: Boolean,
                      altered: Boolean,
                      price: Option[Double],
                      externalId: Option[Long]
                    )

object CsvFormat {
  def parse(obj: JsonObject): CsvFormat = {
    CsvFormat(
      obj.getInt("qty"),
      obj.getString("name"),
      Option(obj.getString("editionCode", null)),
      obj.getBoolean("foil"),
      Condition.parse(obj.get("condition").asJsonObject),
      Language.parse(obj.get("language").asJsonObject),
      obj.getString("board"),
      Option(obj.getJsonNumber("cardId")).map(_.intValue),
      Option(obj.getString("editionName", null)),
      obj.getBoolean("signed", false),
      obj.getBoolean("altered", false),
      Option(obj.getJsonNumber("price")).map(_.doubleValue),
      Option(obj.getJsonNumber("externalId")).map(_.longValue)
    )
  }
}
