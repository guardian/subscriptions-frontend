package utils

import scalaz.OptionT

import scala.concurrent.{ExecutionContext, Future}

trait OptionTSyntax {

  // Provides pure() method on OptionT companion object similar to one provided by cats:
  // https://typelevel.org/cats/api/cats/data/OptionT$.html#pure[F[_]]:cats.data.OptionT.PurePartiallyApplied[F]
  // No need to make it generic in F, since F will always be a Future in our case.
  implicit class OptionTOps(obj: OptionT.type) {
    def pure[A](data: Future[A])(implicit ec: ExecutionContext): OptionT[Future, A] =
      OptionT[Future, A](data.map(Some.apply))
  }
}
