package com.snapcardster.omnimtg

import org.scalatest.words._
import org.scalatest._

class MKMSpec extends FlatSpec with Matchers {
  "MKM API" should "stock get and delete really deletes" in {
    val controller = new MainController
    controller.readProperties()
    val csv = controller.loadMkmStock()
    csv.shouldNot(equal(""))

    val firstRowCols = csv.split("\n").drop(1).head.split(";")
    val firstId = firstRowCols.head.replaceAll("\"", "").toLong
    controller.deleteFromMkmStock(List(SellerDataChanged("", Some(firstId), None, None)))

    val csv2 = controller.loadMkmStock()
    csv2.shouldNot(equal(""))
    csv2.shouldNot(contain(firstId.toString))

    println(csv)
  }
}
