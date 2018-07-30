package com.snapcardster.omnimtg

import java.io._
import java.nio.file.{Path, Paths}
import java.util.Properties

import com.snapcardster.omnimtg.Interfaces.{MainControllerInterface, NativeFunctionProvider, StringProperty}

object DesktopFunctionProvider extends NativeFunctionProvider {
  private val configPath: Path = Paths.get("secret.properties")

  override def openLink(url: String): Unit = Unit

  override def save(prop: Properties, nativeBase: Object): Throwable = {
    var str: OutputStream = new ByteArrayOutputStream()
    try {
      str = new FileOutputStream(configPath.toFile)
      prop.store(str, null)
    } catch {
      case e: Exception => return e
    } finally {
      str.close()
    }
    null
  }

  def updateProp(str: String, property: StringProperty, prop: Properties): Unit = {
    val value = prop.get(str)
    if (value != null && value != "") {
      property.setValue(String.valueOf(value))
    }
  }

  override def readProperties(prop: Properties, controller: MainControllerInterface, nativeBase: Object): Throwable = {
    if (!configPath.toFile.exists) {
      configPath.toFile.createNewFile
    }

    var str: InputStream = new ByteArrayInputStream(Array(42))
    try {
      str = new FileInputStream(configPath.toFile)
      prop.load(str)

      updateProp("mkmApp", controller.getMkmAppToken, prop)
      updateProp("mkmAppSecret", controller.getMkmAppSecret, prop)
      updateProp("mkmAccessToken", controller.getMkmAccessToken, prop)
      updateProp("mkmAccessTokenSecret", controller.getMkmAccessTokenSecret, prop)
      updateProp("snapUser", controller.getSnapUser, prop)
      updateProp("snapToken", controller.getSnapToken, prop)

      if (prop.get("mkmAppToken") != null) {
        controller.getOutput.setValue("Stored authentication information restored")
      }
    } catch {
      case e: Exception => e
    } finally {
      str.close()
    }
    null
  }
}
