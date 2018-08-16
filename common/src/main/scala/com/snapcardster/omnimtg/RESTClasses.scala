package com.snapcardster.omnimtg


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

case class SellerDataChanged(
                              `type`: String,
                              externalId: java.lang.Long,
                              collectionId: java.lang.Long,
                              info: CsvFormat
                            )

case class MKMCsv(fileName: String, fileContent: String)

case class ImportConfirmation(collectionId: Long, successful: Boolean, added: Boolean, info: String)

case class CsvFormat(
                      qty: Int,
                      name: String,
                      editionCode: String,
                      foil: Boolean,
                      condition: Condition,
                      language: Language,
                      meta: String,
                      cardId: java.lang.Long,
                      editionName: String,
                      signed: Boolean,
                      altered: Boolean,
                      price: java.lang.Double,
                      externalId: java.lang.Long,
                      collectionId: java.lang.Long
                    )
