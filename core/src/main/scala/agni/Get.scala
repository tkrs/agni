package agni

import cats.{ Foldable, MonadError }
import cats.syntax.either._
import cats.syntax.option._
import com.datastax.driver.core.{ ProtocolVersion, ResultSet, Row }

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

trait Get[A] {
  def apply[F[_], E](result: ResultSet, version: ProtocolVersion)(
    implicit
    F: MonadError[F, E],
    ev: Throwable <:< E
  ): F[A]
}

object Get {

  def apply[A](implicit A: Get[A]): Get[A] = A

  implicit val getUnit: Get[Unit] = new Get[Unit] {
    override def apply[F[_], E](result: ResultSet, version: ProtocolVersion)(
      implicit
      F: MonadError[F, E],
      ev: <:<[Throwable, E]
    ): F[Unit] = F.pure(())
  }

  implicit def getOneUnsafe[A](implicit A: RowDecoder[A]): Get[A] = new Get[A] {
    override def apply[F[_], E](result: ResultSet, version: ProtocolVersion)(
      implicit
      F: MonadError[F, E],
      ev: <:<[Throwable, E]
    ): F[A] =
      F.catchNonFatal(A(result.one, version).fold(throw _, identity))
  }

  implicit def getOne[A](implicit A: RowDecoder[A]): Get[Option[A]] = new Get[Option[A]] {
    override def apply[F[_], E](result: ResultSet, version: ProtocolVersion)(
      implicit
      F: MonadError[F, E],
      ev: <:<[Throwable, E]
    ): F[Option[A]] = {
      val row = result.one
      if (row == null) F.pure(none)
      else A(row, version).fold(F.raiseError(_), a => F.pure(a.some))
    }
  }

  implicit def getCBF[A: RowDecoder, C[_]](
    implicit
    A: RowDecoder[A],
    cbf: CanBuildFrom[Nothing, A, C[A]]
  ): Get[C[A]] = new Get[C[A]] {
    override def apply[F[_], E](result: ResultSet, version: ProtocolVersion)(
      implicit
      F: MonadError[F, E],
      ev: <:<[Throwable, E]
    ): F[C[A]] = {
      val f = Foldable.iteratorFoldM[F, Row, mutable.Builder[A, C[A]]](result.iterator.asScala, cbf.apply) {
        case (b, a) => A(a, version).fold(F.raiseError(_), a => { b += a; F.pure(b) })
      }
      F.map(f)(_.result())
    }
  }

  implicit val getRowIterator: Get[Iterator[Row]] = new Get[Iterator[Row]] {
    override def apply[F[_], E](result: ResultSet, version: ProtocolVersion)(
      implicit
      F: MonadError[F, E],
      ev: Throwable <:< E
    ): F[Iterator[Row]] =
      F.catchNonFatal(result.iterator.asScala)
  }

  implicit val getRowJavaStream: Get[java.util.stream.Stream[Row]] = new Get[java.util.stream.Stream[Row]] {
    override def apply[F[_], E](result: ResultSet, version: ProtocolVersion)(
      implicit
      F: MonadError[F, E],
      ev: Throwable <:< E
    ): F[java.util.stream.Stream[Row]] =
      F.catchNonFatal(java.util.stream.StreamSupport.stream(result.spliterator(), false))
  }
}
