package util

import scala.util.Try
import scala.concurrent.Future

import play.api.mvc._
import play.api.http.HeaderNames
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Provides request logging with the path and the response time. If a reporter is not provided then logs
 * are written to a "request-logger" logger at info level.
 */
trait LoggedAction extends ActionBuilder[Request] {
  def defaultReporter(msg: String): Unit

  private val delimiter = " : "

  private val ipAddressHeader = "X-Gilt-Remote-Ip"

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    val startTime = System.currentTimeMillis()

    val futureResult = block(request)

    futureResult.onComplete { result =>
      val responseTime = System.currentTimeMillis() - startTime
      defaultReporter(createLogMessage(request, result, responseTime))
    }

    futureResult
  }

  private[util] def createLogMessage(request: Request[_], result: Try[Result], responseTime: Long): String = {
    Array(
      "Request",
      request.headers.get(ipAddressHeader).getOrElse("UnknownIp"),
      request.uri,
      request.headers.get(HeaderNames.ACCEPT).getOrElse("UnknownAcceptHeader"),
      result.map(simpleResult => parseStatusCode(simpleResult).toString).getOrElse("UnknownStatusCode"),
      responseTime).mkString(delimiter)
  }

  private def parseStatusCode(result: Result): Option[Int] = result.header match {
    case ResponseHeader(status, _) =>
      Some(status)
    case _ =>
      None
  }
}

object LoggedAction extends LoggedAction {
  override def defaultReporter(msg: String) = Logger("request-logger").info(msg)
}
