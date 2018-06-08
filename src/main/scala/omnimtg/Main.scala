package omnimtg

import java.io.ByteArrayInputStream
import java.util.Base64

import com.jfoenix.controls.{JFXButton, JFXSlider, JFXTextArea, JFXTextField}
import javafx.application._
import javafx.beans.binding.Binding
import javafx.beans.property.{Property, SimpleIntegerProperty}
import javafx.geometry.Insets
import javafx.scene._
import javafx.scene.control.{Hyperlink, Label, Labeled, TextInputControl}
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{GridPane, Region, VBox}
import javafx.stage.Stage

object Main {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[MainGUI], args: _*)
  }
}

class MainGUI extends Application {
  val controller: MainController = new MainController
  val buttonCss = "-jfx-button-type: RAISED; -fx-background-color: blue; -fx-text-fill: white;"
  val button2Css = "-jfx-button-type: FLAT; -fx-background-color: green; -fx-text-fill: white;"
  var paneCss: String = "-fx-background-color:WHITE;-fx-padding:40;"

  var imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAPIAAADyCAYAAAB3aJikAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyNpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNS1jMDIxIDc5LjE1NDkxMSwgMjAxMy8xMC8yOS0xMTo0NzoxNiAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkVGNzE4MzI1MjI1RDExRTU5NUI0QUM2NTI5NUVBMDQxIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkVGNzE4MzI2MjI1RDExRTU5NUI0QUM2NTI5NUVBMDQxIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6RUY3MTgzMjMyMjVEMTFFNTk1QjRBQzY1Mjk1RUEwNDEiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6RUY3MTgzMjQyMjVEMTFFNTk1QjRBQzY1Mjk1RUEwNDEiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz5VIJpRAAAPrElEQVR42uydCZAU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"

  def pad(x: Region): Unit = x.setPadding(new Insets(2.0))

  def linkTo(field: TextInputControl, prop: Property[String]): Unit = {
    field.textProperty().bindBidirectional(prop)
  }

  def linkTo(field: TextInputControl, prop: Binding[String]): Unit = {
    field.textProperty().bind(prop)
  }

  def linkTo(field: JFXSlider, prop: Property[Number]): Unit = {
    field.valueProperty().bindBidirectional(prop)
  }

  def bindSave(x: JFXButton): Unit = {
    x.setOnMouseClicked(_ => controller.save())
  }

  override def start(primaryStage: Stage): Unit = {
    controller.start()

    primaryStage.setOnCloseRequest(_ => {
      controller.output.setValue("Closing...")
      controller.aborted.setValue(true)
      controller.thread.interrupt()
    })

    val grid = new GridPane
    grid.setHgap(20.0)
    grid.add(new Label("🎚 Sync Options"), 0, 0)
    grid.add(
      set(new JFXSlider(1, 1000, controller.interval.getValue.doubleValue))(linkTo(_, controller.interval)),
      0, 1, 2, 1
    )
    grid.add(new Label("🕓 Sync interval in minutes"), 0, 2)
    grid.add(set(new JFXTextField()) { x =>
      linkTo(x, controller.interval.asString())
      x.setDisable(true)
    }, 1, 2)
    grid.add(new Label("📆 Resulting number of syncs per day"), 0, 3)
    grid.add(set(new JFXTextField()) { x =>
      linkTo(x, new SimpleIntegerProperty(1440).divide(controller.interval).asString())
      x.setDisable(true)
    }, 1, 3)

    val title = "Omni MTG Sync Tool"
    val main = set(new VBox(
      set(new JFXButton("🔄 Start Sync"))(x => {
        x.setStyle(buttonCss)
        val fields = List(
          controller.mkmAppToken, controller.mkmAppSecret, controller.mkmAccessToken, controller.mkmAccessTokenSecret,
          controller.snapUser, controller.snapToken
        )
        x.disableProperty.bind(fields.map(_.isEmpty).reduce(_ or _))
        x.setOnMouseClicked(_ => {
          controller.running.setValue(!controller.running.getValue)
          val txt =
            if (controller.running.getValue)
              "▶ Running, click to stop"
            else
              "⏸ Stopped, click to start"
          x.setText(txt)
        })
      }),
      grid,
      new Label("📜 Output"),
      set(new JFXTextArea("..."))(x => {
        linkTo(x, controller.output)
        x.setEditable(false)
      })
    ))(vbox => vbox.setSpacing(10))

    val mkm = new VBox(
      new Label("🔒 MKM Api Key"),
      set(new Hyperlink("https://cardmarket.com/en/Magic/MainPage/showMyAccount"))(_.setOnMouseClicked(x => handleClick(x))),
      pasteButton("mkm"),
      set(new JFXTextField())(linkTo(_, controller.mkmAppToken)),
      set(new JFXTextField())(linkTo(_, controller.mkmAppSecret)),
      set(new JFXTextField())(linkTo(_, controller.mkmAccessToken)),
      set(new JFXTextField())(linkTo(_, controller.mkmAccessTokenSecret)),
      saveBtn()
    )

    val snap = new VBox(
      new Label("🔒 Snapcardster Api Key"),
      set(new Hyperlink("https://snapcardster.com/app"))(_.setOnMouseClicked(x => handleClick(x))),
      pasteButton("snap"),
      set(new JFXTextField("User"))(linkTo(_, controller.snapUser)),
      set(new JFXTextField("Token"))(linkTo(_, controller.snapToken)),
      saveBtn()
    )

    val pane = new GridPane
    val titleLabel = new Label(title)
    titleLabel.setScaleX(1.4)
    titleLabel.setScaleY(1.4)

    val link = set(new Hyperlink("https://snapcardster.github.io")) { x =>
      x.setOnMouseClicked(x => handleClick(x))
    }

    pane.add(new VBox(titleLabel, link), 0, 0, 2, 1)
    pane.add(set(mkm)(pad), 0, 1)
    pane.add(set(snap)(pad), 1, 1)
    pane.add(set(main)(pad), 0, 2, 2, 1)
    pane.setPadding(new Insets(20.0))

    primaryStage.setTitle(title)
    primaryStage.getIcons.add(new Image(new ByteArrayInputStream(Base64.getDecoder.decode(imageBase64))))

    pane.setStyle(paneCss)

    primaryStage.setScene(new Scene(pane))
    // TODO for debugging only:
    // controller.loadSnapChangedAndDeleteFromStock
    primaryStage.show()
  }

  def pasteButton(mode: String): JFXButton = {
    set(new JFXButton("📋 Paste from Clipboard"))(x => {
      x.setStyle(button2Css)
      x.setOnMouseClicked(_ => controller.insertFromClip(mode))
    })
  }

  def saveBtn(): JFXButton = {
    set(new JFXButton("💾 Save"))(x => {
      x.setStyle(buttonCss)
      bindSave(x)
    })
  }

  def handleClick(event: MouseEvent, linkOrNull: String = null): Unit = {
    (event.getSource, linkOrNull) match {
      case (x: Labeled, null) => controller.openLink(x.getText)
      case (_, link) => controller.openLink(link)
    }
  }

  def set[T <: Node, V](node: T)(modify: T => V): T = {
    modify(node)
    node
  }
}
