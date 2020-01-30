package omnimtg

case class Condition(
                      longString: String,
                      shortString: String,
                      code: Int,
                      usString: String
                    )

case class Language(
                     longString: String,
                     shortString: String,
                     code: Int
                   )

case class Token(token: String)

case class MKMSomething(stock: String)

case class MKMProductsfile(productsfile: String)

case class MKMExpansions(expansion: Array[MKMExpansion])

case class MKMExpansion(idExpansion: Long, enName: String)

case class MKMProductEntry(
                            idProduct: Long, enName: String,
                            idExpansion: Long, expansionName: String
                          )

case class ScryfallList(
                         data: Array[ScryfallCard]
                       )

case class ScryfallCard(name: String)

case class SellerDataChanged(
                              `type`: String,
                              externalId: Long,
                              collectionId: java.lang.Long,
                              info: CsvFormat
                            )

case class MtgCsvFileRequest(
                   fileName: String,
                   fileContent: String,
                   bidPriceMultiplier: java.lang.Double = null,
                   minBidPrice: java.lang.Double = null,
                   maxBidPrice: java.lang.Double = null,
                   askPriceMultiplier: java.lang.Double = null,
                   info: String
                 )

case class ImportConfirmation(
                               collectionId: Long,
                               successful: Boolean,
                               added: Boolean,
                               info: String
                             )

case class MkmConfirm(
                       externalId: Long,
                       count: Int,
                       successful: Boolean,
                       message: String
                     )

case class CsvFormat(
                      qty: Int,
                      name: String,
                      editionCode: String,
                      foil: Boolean,
                      condition: Condition,
                      language: Language,
                      meta: String,
                      cardId: Long,
                      editionName: String,
                      signed: Boolean,
                      altered: Boolean,
                      price: Double,
                      externalId: Long,
                      collectionId: Long
                    )
