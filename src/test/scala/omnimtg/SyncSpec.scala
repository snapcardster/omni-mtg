package omnimtg

import javax.json.{JsonObject, JsonValue}
import org.scalatest._

class SyncSpec extends FlatSpec with Matchers {
  val buyerAuth: String = "testusernowallet,e61ce61fd6825b1c116babc9b1a387936981548437c05a0163e5e621637f5378"
  val sellerAuth: String = "testgenericseller,a9ecd25a8a5e96062395cbb97fdbefff34ac034077a3c44bd5a86fdcb808bccc"
  val hardCodedBuyerAddressId: Long = 257L

  /*"Snap API" should "sync mkm into snap should lead to empty changed list" in {
    implicit val connector: SnapConnector = new SnapConnector
    implicit val controller: MainController = new MainController
    controller.readProperties

    // load from mkm
    val csv = controller.loadMkmStock
    println("csv with " + csv.count(_ == '\n') + " lines read from mkm api")

    // sync into snap
    val status = controller.postToSnap(csv)
    println(status)

    // check changes should be empty
    val changed = controller.loadChangedFromSnap()
    changed shouldEqual "[]"
  }*/

  "Sync Logic" should "work in the full process (see comments)" in {
    implicit val connector: SnapConnector = new SnapConnector
    implicit val controller: MainController = new MainController
    controller.readProperties()

    // delete everything collection related from seller user
    var url = controller.snapBaseUrl + "/collection?priceSource=ck"
    var res = connector.call(url, "GET", sellerAuth)

    var arr = controller.asJsonArray(controller.fromJson(res))
    val allIds = arr.map(_.asJsonObject).map(x => getLong(x, "id"))

    // TODO: remove this code, this deletes all mkm offers:
    // val extId = arr.map(_.asJsonObject).map(x => getLong(x, "externalId")).toList
    // controller.deleteFromMkmStock(extId)

    val inBlocks = allIds.grouped(128)
    // to get around error "URI length exceeds the configured limit of 2048 characters"
    for (block <- inBlocks) {
      val ids = block.mkString(",")
      url = controller.snapBaseUrl + "/collection/" + ids
      res = connector.call(url, "DELETE", sellerAuth)
      println(res)
      url = controller.snapBaseUrl + "/offers/" + ids
      res = connector.call(url, "DELETE", sellerAuth)
      println(res)
    }

    // check changes should be empty (there's nothing to sync)
    var changed = controller.loadChangedFromSnap()
    changed shouldEqual "[]"

    // get collection for lookup
    url = controller.snapBaseUrl + "/collection?priceSource=ck"
    res = connector.call(url, "GET", sellerAuth)
    res shouldEqual "[]"

    var csv = controller.loadMkmStock
    println("csv with " + csv.count(_ == '\n') + " lines read from mkm api")

    // sync into snap
    var status = controller.postToSnap(csv)
    println(status)

    // check changes should be empty (just synced and nothing happened in between)
    changed = controller.loadChangedFromSnap()
    changed shouldEqual "[]"

    // put card into cart and checkout
    val (offerId, shipmentCardId) = putSomeCardIntoCartAndCheckout()

    // get collection for lookup
    url = controller.snapBaseUrl + "/collection?priceSource=ck"
    res = connector.call(url, "GET", sellerAuth)
    res shouldNot equal("[]")

    arr = controller.asJsonArray(controller.fromJson(res))
    val collItem = arr.map(_.asJsonObject).find(x => getLong(x, "id") == offerId).get
    val collItemExtId = getLong(collItem, "externalId")

    // it is now considered reserved and is returned from sync route
    res = controller.loadChangedFromSnap()
    arr = controller.asJsonArray(controller.fromJson(res))

    println(res)
    res shouldNot equal("[]")

    var changeItem = findChangeItem(arr, collItemExtId)

    // check changes contain reserved item
    var changeType = changeItem.getString("type")
    changeType shouldEqual "reserved"

    // then deleting this item at mkm

    // get line before delete
    val csvLinesBeforeDelete = csv.split("\n").find(_.contains(collItemExtId.toString)).get.replaceAll("\"", "")

    val idsToDelete = arr.map(x => SellerDataChanged.parse(x.asJsonObject)).filter(x => x.`type` == "reserved").toList
    status = controller.deleteFromMkmStock(idsToDelete)
    println(status)

    // should really delete it
    waitSomeTime()
    csv = controller.loadMkmStock()
    // Get csv line (that ends with qty;onSaleFlag)
    val csvLinesAfterDelete = csv.split("\n").find(_.contains(collItemExtId.toString)).map(_.replaceAll("\"", ""))
    if (csvLinesAfterDelete.isEmpty) { // line can be fully deleted if one was left
      csvLinesBeforeDelete.endsWith(";1;1") shouldEqual true
    } else { // line should not be fully deleted if more than one was left
      csvLinesBeforeDelete.endsWith(";1;1") shouldEqual false
      // and it must be changed after successful delete
      csvLinesAfterDelete.get shouldNot equal(csvLinesBeforeDelete)
    }


    // searching in purchases for this id
    url = controller.snapBaseUrl + "/marketplace/bundle/purchases"
    res = connector.call(url, "GET", buyerAuth)
    println(res)
    res shouldNot equal("")

    val json = controller.fromJson(res).asJsonObject
    val bundles = controller.asJsonArray(json.getJsonArray("bundles"))
    val mapped = bundles.map(_.asJsonObject).map { x =>
      getLong(x.getJsonObject("bundle"), "saleId") -> controller.asJsonArray(x.asJsonObject.getJsonArray("cards"))
    }

    // to find the bundle
    val bundleWithCard = mapped.find(_._2.exists(x => getLong(x.asJsonObject, "shipmentCardId") == shipmentCardId))
    bundleWithCard shouldNot equal(None)
    val saleId = bundleWithCard.get._1

    // then cancelling it
    url = controller.snapBaseUrl + "/marketplace/bundle/" + saleId + "/markAsCancelled"
    res = connector.call(url, "POST", buyerAuth)
    println(res)
    res shouldNot equal("")

    // show now that the sync also shows the card to-be-added again
    res = controller.loadChangedFromSnap()
    arr = controller.asJsonArray(controller.fromJson(res))

    println(res)
    res shouldNot equal("[]")

    changeItem = findChangeItem(arr, collItemExtId)
    changeType = changeItem.getString("type")
    changeType shouldEqual "added"

    val items = arr.toList.map(x => SellerDataChanged.parse(x.asJsonObject)).filter(x => x.`type` == "added")

    val info = changeItem.getJsonObject("info")
    val csvItemToAdd = CsvFormat.parse(info)

    status = controller.addToMkmStock(items)
    println(status)

    waitSomeTime()

    csv = controller.loadMkmStock()
    val csvLinesAfterAdd = csv.split("\n").find(_.contains(csvItemToAdd.name)).get.replaceAll("\"", "")
    if (csvLinesAfterDelete.isEmpty) { // either removed, then only one was left
      csvLinesBeforeDelete.endsWith(";1;1") shouldEqual true
      csv.contains(csvItemToAdd.name) shouldEqual true
    } else { // or the line has changed and there was more than one
      csvLinesBeforeDelete.endsWith(";1;1") shouldEqual false
      csvLinesAfterAdd shouldNot equal(csvLinesAfterDelete.get)
      csvLinesAfterAdd shouldEqual csvLinesBeforeDelete
    }
  }

  /** don't know why but getting the stock immediately doesn't reflect changes, so just wait some seconds */
  def waitSomeTime(): Unit = {
    val sec = 12
    Thread.sleep(sec * 1000)
  }

  def findChangeItem(arr: Array[JsonValue], collItemExtId: Long): JsonObject = {
    arr.find(x => getLong(x.asJsonObject, "externalId") == collItemExtId).get.asJsonObject
  }

  def getLong(collItem: JsonObject, key: String): Long = {
    collItem.getJsonNumber(key).longValue
  }

  /** returns offerId -> shipmentCardId */
  def putSomeCardIntoCartAndCheckout()(implicit controller: MainController, connector: SnapConnector): (Long, Long) = {
    // get an mkm offer (testgenericseller only has mkm offers)
    var url = controller.snapBaseUrl + "/marketplace/search/bySeller/detail/" + controller.snapUser.getValue
    var res = connector.call(url, "GET", buyerAuth)

    //println(res)
    res.shouldNot(equal("[]"))

    val arr = controller.asJsonArray(controller.fromJson(res))
    val firstFree = arr.map(_.asJsonObject).find(x => getLong(x, "offerReservedUntil") == 0L).get
    val offerId = firstFree.getJsonNumber("offerId").longValue

    // put into cart
    url = controller.snapBaseUrl + "/marketplace/cart/offer"
    res = connector.call(url, "PUT", buyerAuth, controller.jsonFromMap(Map("offerId" -> offerId)))

    val shipmentCardId = controller.fromJson(res).asJsonObject.getJsonNumber("shipmentCardId").longValue
    println(res)
    res.shouldNot(equal(""))

    // checkout
    url = controller.snapBaseUrl + "/marketplace/cart/checkout"
    val map = controller.jsonFromMap(Map("shippingAddressId" -> hardCodedBuyerAddressId, "useSnapcoins" -> false))
    res = connector.call(url, "POST", buyerAuth, map)

    println(res)
    res.shouldNot(equal(""))

    offerId -> shipmentCardId
  }
}
