package com.snapcardster.omnimtg

import com.snapcardster.omnimtg.Interfaces.NativeFunctionProvider

object DesktopFunctionProvider extends NativeFunctionProvider {
  override def openLink(url: String): Unit = Unit
}
