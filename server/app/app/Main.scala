package app

class Main {}

object MainApp extends App {
  Main.main(args)
}

object Main {
  def main(args: Array[String]): Unit = {
    //if (!args.contains("headless")) {
    javafx.application.Application.launch(classOf[omnimtg.MainGUI], args: _*)
    /*} else {

    import controllers.ServerMainController
    import java.io.{BufferedReader, File, InputStreamReader}
    import play.core.server.{AkkaHttpServer, ServerConfig}
    import akka.actor.ActorSystem
    import akka.stream.{ActorMaterializer, Materializer, _}
    import play.api.{DefaultApplication, Mode, Configuration, _}
    import play.api.http._
    import play.api.i18n.{DefaultLangs, DefaultMessagesApi, _}
    import play.api.inject.{DefaultApplicationLifecycle, Injector, NewInstanceInjector, SimpleInjector}
    import play.api.mvc.{PlayBodyParsers, _}
    import play.api.mvc.request.RequestFactory
    import play.api.routing.Router
    import play.api.routing.sird._

    import scala.concurrent.ExecutionContext

      val confFile = new File(args.headOption.getOrElse("conf/application.conf"))
      println(confFile.getAbsolutePath)
      val classLoader = Main.getClass.getClassLoader
      val modeVal = Mode.Dev
      val environment = new Environment(confFile, classLoader, modeVal)
      val conf = Configuration.load(environment)

      val injector: Injector =
        new SimpleInjector(NewInstanceInjector)
      /*+ users + router + cookieSigner + csrfTokenSigner + httpConfiguration + tempFileCreator + global*/

      val ex = ExecutionContext.global
      implicit val system: ActorSystem = ActorSystem("actors")
      val mat: Materializer = ActorMaterializer()
      val parsers = PlayBodyParsers.apply()(mat)
      val cc = DefaultControllerComponents(
        new DefaultActionBuilderImpl(parsers.defaultBodyParser)(ex),
        parsers,
        new DefaultMessagesApi(),
        new DefaultLangs(Nil),
        new DefaultFileMimeTypes(FileMimeTypesConfiguration.apply(Map("text" -> "application/text"))),
        ex)
      val mainController = new ServerMainController(cc, ex)
      val routerVal: Router = Router.from {
        case GET(p"/logs") => mainController.getLogs
        case GET(p"/status") => mainController.getStatus
        case GET(p"/settings") => mainController.getSettings
        case POST(p"/settings") => mainController.postSettings
        case POST(p"/exitRequest") => mainController.postExitRequest
      }
      // val eh: HttpErrorHandler = new ErrorHandler()(ex)
      val components = null // new MappedJavaHandlerComponents
      val errHandler: HttpErrorHandler = new DefaultHttpErrorHandler(environment, conf, None, Some(routerVal))
      val handler: HttpRequestHandler = new JavaCompatibleHttpRequestHandler(routerVal, errHandler, HttpConfiguration(), HttpFilters(), components)

      val application = new DefaultApplication(
        environment,
        new DefaultApplicationLifecycle(),
        injector, conf,
        RequestFactory.plain,
        handler,
        errHandler, system, mat
      )

      Play.start(application)
      //println(Await.result(mainController.getStatus.apply(null), Duration.Inf))
      val portVal = conf.underlying.getInt("serverPort")
      AkkaHttpServer.fromApplication(
        application, ServerConfig(classLoader, new File("."), port = Some(portVal), mode = modeVal)
      )
      println("Started, press Enter to exit")
      try {
        new BufferedReader(new InputStreamReader(System.in)).readLine()
      } catch {
        case x: Throwable =>
          x.printStackTrace()
      }
      Play.stop(application)
    }*/
  }
}