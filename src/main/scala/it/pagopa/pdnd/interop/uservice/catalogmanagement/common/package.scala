package it.pagopa.pdnd.interop.uservice.catalogmanagement

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

package object common {
  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {
    def toFuture(error: Throwable): Future[A] = option.map(Future.successful).getOrElse(Future.failed[A](error))
  }

  implicit class EitherOps[E <: Throwable, A](val either: Either[E, A]) extends AnyVal {
    def toFuture: Future[A] = either.fold[Future[A]](Future.failed[A], Future.successful)
  }

  implicit class StringOps(val str: String) extends AnyVal {
    def parseUUID: Either[Throwable, UUID] = Try { UUID.fromString(str) }.toEither
  }
}
