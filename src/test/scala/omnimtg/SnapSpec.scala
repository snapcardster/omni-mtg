package omnimtg

import org.scalatest._

class SnapSpec extends FlatSpec with Matchers {
  "Snap API" should "return changes" in {
    val controller = new MainController
    controller.readProperties

    val res = controller.loadChangedFromSnap()

    println(res)
    res.shouldNot(equal(""))
  }
}
