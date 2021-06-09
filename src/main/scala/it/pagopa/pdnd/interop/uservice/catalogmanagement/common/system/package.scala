package it.pagopa.pdnd.interop.uservice.catalogmanagement.common

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.{Missing, Provided}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt

package object system {

  implicit val timeout: Timeout = 300.seconds

  object Authenticator extends Authenticator[Map[String, String]] {

    override def apply(credentials: Credentials): Option[Map[String, String]] = {
      credentials match {
        case Provided(identifier) => Some(Map("bearer" -> identifier))
        case Missing              => None
      }
    }

  }
}
