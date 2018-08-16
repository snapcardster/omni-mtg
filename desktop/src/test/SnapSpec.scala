package omnimtg

import com.snapcardster.omnimtg.{DesktopFunctionProvider, JavaFXPropertyFactory, MainController}
import org.scalatest._

class SnapSpec extends FlatSpec with Matchers {
  "Snap API" should "return changes" in {
    val controller = new MainController(JavaFXPropertyFactory, DesktopFunctionProvider)
    controller.readProperties(DesktopFunctionProvider)

    val res = controller.loadChangedFromSnap()

    println(res)
    res.shouldNot(equal(""))
  }
}