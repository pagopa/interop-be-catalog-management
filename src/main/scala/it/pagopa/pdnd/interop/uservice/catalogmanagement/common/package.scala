package it.pagopa.pdnd.interop.uservice.catalogmanagement

import scala.concurrent.Future

package object common {
  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {
    def toFuture(error: Throwable): Future[A] = option.map(Future.successful).getOrElse(Future.failed[A](error))

  }
}
