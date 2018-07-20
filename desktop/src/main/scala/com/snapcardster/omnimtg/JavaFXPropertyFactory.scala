package com.snapcardster.omnimtg

import java.lang
import java.util.Properties

import com.snapcardster.omnimtg.Interfaces.{BooleanProperty, IntegerProperty, PropertyFactory, StringProperty}
import javafx.beans.property.{SimpleBooleanProperty, SimpleIntegerProperty, SimpleStringProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}

object JavaFXPropertyFactory extends PropertyFactory {
  override def newBooleanProperty(initialValue: lang.Boolean): BooleanProperty = new JavaFXBooleanProperty(initialValue)

  override def newStringProperty(initialValue: String): StringProperty = new JavaFXStringProperty(initialValue)

  override def newIntegerProperty(initialValue: Integer): IntegerProperty = new JavaFXIntegerProperty(initialValue)

  override def newListener(prop: Properties, name: String): AnyRef = {
    val l = new ChangeListener[Any] {
      def changed(observable: ObservableValue[_], oldValue: Any, newValue: Any): Unit = {
        prop.put(name, String.valueOf(newValue))
      }
    }
    l
  }
}

class JavaFXBooleanProperty extends BooleanProperty {
  var nativeBase: SimpleBooleanProperty = _

  def this(value: Boolean) {
    this()
    this.nativeBase = new SimpleBooleanProperty(value)
  }

  override def setValue(value: lang.Boolean): Unit = nativeBase.set(value)

  override def getValue: lang.Boolean = nativeBase.getValue

  override def getNativeBase: SimpleBooleanProperty = nativeBase
}

class JavaFXStringProperty extends StringProperty {
  var nativeBase: SimpleStringProperty = _

  def this(value: String) {
    this()
    this.nativeBase = new SimpleStringProperty(value)
  }

  override def setValue(value: lang.String): Unit = nativeBase.set(value)

  override def getValue: String = nativeBase.getValue

  override def getNativeBase: SimpleStringProperty = nativeBase

  override def addListener(listener: Object): Unit = nativeBase.addListener(listener.asInstanceOf[ChangeListener[Any]])
}

class JavaFXIntegerProperty extends IntegerProperty {
  var nativeBase: SimpleIntegerProperty = _

  def this(value: Integer) {
    this()
    this.nativeBase = new SimpleIntegerProperty(value)
  }

  override def setValue(value: Integer): Unit = nativeBase.set(value)

  override def getValue: Integer = nativeBase.getValue

  override def getNativeBase: SimpleIntegerProperty = nativeBase

  override def set(value: Integer): Unit = nativeBase.set(value)
}