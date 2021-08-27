package it.pagopa.pdnd.interop.uservice.catalogmanagement

import scala.concurrent.Future

package object common {
  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {
    def toFuture(error: Throwable): Future[A] = option.map(Future.successful).getOrElse(Future.failed[A](error))
  }

  implicit class EitherOps[E <: Throwable, A](val either: Either[E, A]) extends AnyVal {
    def toFuture: Future[A] = either.fold[Future[A]](Future.failed[A], Future.successful)
  }
}
