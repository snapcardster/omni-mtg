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

    // check changes should be empty

    //val changed = controller.loadChangedFromSnap()
    //changed shouldEqual "[]"

    // put card into cart and checkout
    val (offerId, shipmentCardId) = putSomeCardIntoCartAndCheckout()

    // get collection for lookup
    var url = controller.snapBaseUrl + "/collection?priceSource=ck"
    var res = connector.call(url, "GET", sellerAuth)
    res shouldNot equal("[]")

    var arr = controller.asJsonArray(controller.fromJson(res))
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
    var status = controller.deleteFromMkmStock(List(collItemExtId))
    println(status)

    // should really delete it
    var csv = controller.loadMkmStock()
    csv shouldNot contain(collItemExtId.toString)

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


    // show now let sync considered the card to-be-added again which is returned from sync route
    res = controller.loadChangedFromSnap()
    arr = controller.asJsonArray(controller.fromJson(res))

    println(res)
    res shouldNot equal("[]")

    changeItem = findChangeItem(arr, collItemExtId)
    changeType = changeItem.getString("type")
    changeType shouldEqual "added"

    val info = changeItem.getJsonObject("info")
    val csvItemToAdd = CsvFormat.parse(info)

    // check old csv where item was removed
    csv shouldNot contain(csvItemToAdd.name)

    status = controller.addToMkmStock(List(csvItemToAdd))
    println(status)

    // don't know why but getting the stock immediately doesn't reflect the add, so just wait some sec
    val sec = 12
    Thread.sleep(sec * 1000)

    csv = controller.loadMkmStock()
    csv.contains(csvItemToAdd.name) shouldEqual true
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
    res.shouldNot(equal(""))

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
