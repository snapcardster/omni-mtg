package omnimtg

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.ByteArrayInputStream
import java.util.Base64
import omnimtg.Interfaces._
import omnimtg._
import com.jfoenix.controls._
import javafx.application._
import javafx.beans.binding.Binding
import javafx.beans.property.{Property, SimpleIntegerProperty, SimpleStringProperty}
import javafx.geometry.Insets
import javafx.scene._
import javafx.scene.control._
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.text.Font
import javafx.stage.Stage

import scala.util.Try

class MainGUI extends Application {
  val controller: MainController = new MainController(JavaFXPropertyFactory, DesktopFunctionProvider)
  val buttonCss = "--button-type: RAISED; -fx-background-color: blue; -fx-text-fill: white;"
  val button2Css = "--button-type: FLAT; -fx-background-color: green; -fx-text-fill: white;"
  var paneCss: String = "-fx-background-color:WHITE;-fx-padding:40;"

  var imageBase64 = SnapHelpers.iconBase64

  def pad(x: Region): Unit = x.setPadding(new Insets(1.0))

  def linkTo(field: TextInputControl, prop: Property[String]): Unit = {
    field.textProperty().bindBidirectional(prop)
  }

  def linkTo(field: TextInputControl, prop: Binding[String]): Unit = {
    field.textProperty().bind(prop)
  }

  def linkTo(field: Slider, prop: Property[Number]): Unit = {
    field.valueProperty().bindBidirectional(prop)
  }

  def bindSave(x: Button): Unit = {
    x.setOnMouseClicked(_ => controller.save(null))
  }

  def bindLogin(x: Button): Unit = {
    x.setOnMouseClicked(_ => controller.loginSnap())
  }

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setOnCloseRequest { _ =>
      // x.consume()
      controller.getOutput.setValue("Closing...")
      controller.getAborted.setValue(true)
      controller.getThread.interrupt()
      // System.exit(0)
    }
    val pane = getStage

    primaryStage.setTitle(controller.title)
    primaryStage.getIcons.add(new Image(new ByteArrayInputStream(Base64.getDecoder.decode(imageBase64))))

    primaryStage.setScene(new Scene(pane))
    // TODO for debugging only:
    // controller.loadSnapChangedAndDeleteFromStock
    primaryStage.show()
  }

  def getStage: GridPane = {
    controller.start(DesktopFunctionProvider)

    val grid = new GridPane
    grid.setHgap(20.0)
    grid.add(new Label("🎚 Sync Options"), 0, 0)
    grid.add(
      set(new JFXSlider(20, 43600, controller.getInterval.getValue.doubleValue))(linkTo(_, controller.getInterval.getNativeBase.asInstanceOf[SimpleIntegerProperty])),
      0, 1, 2, 1
    )
    grid.add(new Label("🕓 Sync interval in seconds"), 0, 2)
    grid.add(set(new JFXTextField) { x =>
      linkTo(x, controller.getInterval.getNativeBase.asInstanceOf[SimpleIntegerProperty].asString)
      x.setDisable(true)
    }, 1, 2)
    grid.add(new Label("📆 Resulting number of syncs per day"), 0, 3)
    grid.add(set(new JFXTextField) { x =>
      linkTo(x, new SimpleIntegerProperty(1440 * 60).divide(controller.getInterval.getNativeBase.asInstanceOf[SimpleIntegerProperty]).asString())
      x.setDisable(true)
    }, 1, 3)
    grid.add(new Label("📆 Next sync in"), 0, 4)
    grid.add(set(new JFXTextField) { x =>
      linkTo(x, controller.getNextSync().getNativeBase.asInstanceOf[SimpleIntegerProperty].asString)
      x.setDisable(true)
    }, 1, 4)

    val fields = List(
      controller.getMkmAppToken, controller.getMkmAppSecret, controller.getMkmAccessToken, controller.getMkmAccessTokenSecret,
      controller.getSnapUser, controller.getSnapToken
    )

    val startButton = set(new JFXButton("🔄 Start Sync")) { x =>
      x.setStyle(buttonCss)

      x.disableProperty.bind(fields.map(_.getNativeBase.asInstanceOf[SimpleStringProperty].isEmpty).reduce(_ or _))
      x.setOnMouseClicked { _ =>
        controller.getRunning.setValue(!controller.getRunning.getValue)
        val txt =
          if (controller.getRunning.getValue)
            "▶ Running, click to stop"
          else
            "⏸ Stopped, click to start"
        x.setText(txt)
      }
    }

    if (!fields.map(_.getNativeBase.asInstanceOf[SimpleStringProperty].isEmpty).reduce(_ or _).get()) {
      controller.getRunning.setValue(true)
      startButton.setText("▶ Running, click to stop")
    }

    val main = set(new VBox(
      startButton,
      grid
    ))(vbox => vbox.setSpacing(6))

    val out = set(new VBox(
      new Label("📜 Output"),
      set(new TextArea("...")) { x =>
        linkTo(x, controller.getOutput.getNativeBase.asInstanceOf[SimpleStringProperty])
        x.setEditable(false)
        x.setPrefSize(600, 450)
      }
    ))(vbox => vbox.setSpacing(6))

    val mkm = new VBox(
      new Label("🔒 MKM Api Key"),
      set(new Hyperlink("https://cardmarket.com/en/Magic/MainPage/showMyAccount"))(_.setOnMouseClicked(x => handleClick(x))),
      pasteButton("mkm"),
      set(new JFXTextField) { x =>
        x.setPromptText("Enter MKM App Token")
        linkTo(x, controller.getMkmAppToken.getNativeBase.asInstanceOf[SimpleStringProperty])
      },
      set(new JFXTextField) { x =>
        linkTo(x, controller.getMkmAppSecret.getNativeBase.asInstanceOf[SimpleStringProperty])
        x.setPromptText("Enter MKM App Secret")
      },
      set(new JFXTextField) { x =>
        x.setPromptText("Enter MKM Access Token")
        linkTo(x, controller.getMkmAccessToken.getNativeBase.asInstanceOf[SimpleStringProperty])
      },
      set(new JFXTextField) { x =>
        x.setPromptText("Enter MKM Access Token Secret")
        linkTo(x, controller.getMkmAccessTokenSecret.getNativeBase.asInstanceOf[SimpleStringProperty])
      },
      saveBtn()
    )

    val snap = new VBox(
      new Label("🔒 Snapcardster Credentials"),
      //set(new Hyperlink("https://snapcardster.com/app"))(_.setOnMouseClicked(x => handleClick(x))),
      //pasteButton("snap"),
      set(new JFXTextField) { x =>
        linkTo(x, controller.getSnapUser.getNativeBase.asInstanceOf[SimpleStringProperty])
        x.setPromptText("Enter Snapcardster User Id")
      },
      set(new JFXTextField) { x =>
        linkTo(x, controller.getSnapPassword.getNativeBase.asInstanceOf[SimpleStringProperty])
        x.setPromptText("Enter Snapcardster Password")
      },
      set(new JFXTextField) { x =>
        linkTo(x, controller.getSnapToken.getNativeBase.asInstanceOf[SimpleStringProperty])
        x.setPromptText("Snapcardster Token")
      },
      loginAndGetTokenBtn(),
      saveBtn()
    )

    val pane = new GridPane
    val titleLabel = new Label(controller.title)
    titleLabel.setFont(Font.font(16))

    val link = set(new Hyperlink("https://snapcardster.github.io")) { x =>
      x.setOnMouseClicked(x => handleClick(x))
    }

    pane.add(new VBox(titleLabel, link), 0, 0, 2, 1)
    pane.add(set(mkm)(pad), 0, 1)
    pane.add(set(snap)(pad), 0, 2)
    pane.add(set(main)(pad), 0, 3)
    pane.add(set(out)(x => {
      x.setPadding(new Insets(1.0, 1.0, 1.0, 12.0))
    }), 1, 1, 1, 3)

    pane.setStyle(paneCss)
    pane
  }

  def pasteButton(mode: String): Button = {
    set(new JFXButton("📋 Paste from Clipboard")) { x =>
      x.setStyle(button2Css)
      x.setOnMouseClicked { _ =>
        val data = Try(String.valueOf(Toolkit.getDefaultToolkit.getSystemClipboard.getData(DataFlavor.stringFlavor))).toOption.getOrElse("")
        if (data != "") {
          controller.insertFromClip(mode, data)
        }
      }
    }
  }

  def loginAndGetTokenBtn(): Button = {
    set(new JFXButton("💾 Login and get Token")) { x =>
      x.setStyle(buttonCss)
      bindLogin(x)
    }
  }

  def saveBtn(): Button = {
    set(new JFXButton("💾 Save")) { x =>
      x.setStyle(buttonCss)
      bindSave(x)
    }
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
