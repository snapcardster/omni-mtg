package omnimtg

import omnimtg.Interfaces.PropertyFactory
import java.lang
import java.util.Properties

import javafx.beans.property._
import javafx.beans.value.{ChangeListener, ObservableValue}

object JavaFXPropertyFactory extends omnimtg.Interfaces.PropertyFactory {
  override def newBooleanProperty(initialValue: lang.Boolean) = new JavaFXBooleanProperty(initialValue)

  override def newStringProperty(initialValue: String) = new JavaFXStringProperty(initialValue)

  override def newIntegerProperty(initialValue: Integer) = new JavaFXIntegerProperty(initialValue)

  override def newStringProperty(name: String, value: String, prop: Properties): omnimtg.JavaFXStringProperty = {
    val stringProp = newStringProperty(value)
    val l = new ChangeListener[Any] {
      def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
        prop.put(name, String.valueOf(newValue))
      }
    }
    stringProp.addListener(l)
    stringProp
  }

  override def newIntegerProperty(name: String, initialValue: Integer, prop: Properties) =
    new JavaFXIntegerProperty(initialValue)

  override def newDoubleProperty(name: String, initialValue: java.lang.Double, prop: Properties) =
    new JavaFxDoubleProperty(initialValue)

  override def newObjectProperty(initialValue: Object) = new JavaFXObjectProperty(initialValue)
}

class JavaFXBooleanProperty extends omnimtg.Interfaces.BooleanProperty {
  var nativeBase: SimpleBooleanProperty = _

  def this(value: Boolean) {
    this()
    this.nativeBase = new SimpleBooleanProperty(value)
  }

  override def setValue(value: lang.Boolean): Unit = nativeBase.set(value)

  override def getValue: lang.Boolean = nativeBase.getValue

  override def getNativeBase: SimpleBooleanProperty = nativeBase
}

class JavaFXStringProperty extends omnimtg.Interfaces.StringProperty {
  var nativeBase: SimpleStringProperty = _

  def this(value: String) {
    this()
    this.nativeBase = new SimpleStringProperty(value)
  }

  override def setValue(value: lang.String): Unit = nativeBase.set(value)

  override def getValue: String = nativeBase.getValue

  override def getNativeBase: SimpleStringProperty = nativeBase

  override def addListener(listener: Object): Unit = nativeBase.addListener(listener.asInstanceOf[ChangeListener[Any]])

  override def setValue(value: String, callListener: lang.Boolean): Unit = nativeBase.setValue(value)
}


class JavaFxDoubleProperty extends omnimtg.Interfaces.DoubleProperty {
  var nativeBase: SimpleDoubleProperty = _

  def this(value: java.lang.Double) {
    this()
    this.nativeBase = new SimpleDoubleProperty(value)
  }

  override def setValue(value: java.lang.Double): Unit = nativeBase.set(value)

  override def getValue: java.lang.Double = nativeBase.getValue

  override def getNativeBase: SimpleDoubleProperty = nativeBase

  override def addListener(listener: Any): Unit = ()
}


class JavaFXIntegerProperty extends omnimtg.Interfaces.IntegerProperty {
  var nativeBase: SimpleIntegerProperty = _

  def this(value: Integer) {
    this()
    this.nativeBase = new SimpleIntegerProperty(value)
  }

  override def setValue(value: Integer): Unit = nativeBase.set(value)

  override def getValue: Integer = nativeBase.getValue

  override def getNativeBase: SimpleIntegerProperty = nativeBase

  override def addListener(listener: Any): Unit = ()
}

class JavaFXObjectProperty extends omnimtg.Interfaces.ObjectProperty {
  var nativeBase: SimpleObjectProperty[Object] = _

  def this(value: Object) {
    this()
    this.nativeBase = new SimpleObjectProperty[Object](value)
  }

  override def setValue(value: Object): Unit = nativeBase.set(value)

  override def getValue: Object = nativeBase.getValue

  override def getNativeBase: SimpleObjectProperty[Object] = nativeBase

  def addListener(listener: Any): Unit = ()
}