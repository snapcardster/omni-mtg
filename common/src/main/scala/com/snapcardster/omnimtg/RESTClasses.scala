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

case class SellerDataChanged(
                              `type`: String,
                              externalId: Option[Long],
                              collectionId: Option[Long],
                              info: Option[CsvFormat]
                            )

case class MKMCsv(filename: String, fileContent: String)

case class ImportConfirmation(collectionId: Long, successful: Boolean, added: Boolean, info: String)

case class CsvFormat(
                      qty: Int,
                      name: String,
                      editionCode: Option[String],
                      foil: Boolean,
                      condition: Condition,
                      language: Language,
                      meta: String,
                      cardId: Option[Long],
                      editionName: Option[String],
                      signed: Boolean,
                      altered: Boolean,
                      price: Option[Double],
                      externalId: Option[Long],
                      collectionId: Option[Long]
                    )
