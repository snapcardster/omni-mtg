import java.time.Clock

import com.google.inject.AbstractModule

class Module extends AbstractModule {
  override def configure(): Unit = {
    val fun = new ServerFunctionProvider
    fun.println("Loaded Module.")

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
  }
}