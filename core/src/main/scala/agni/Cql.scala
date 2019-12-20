package agni

import java.util.concurrent.CompletionStage

import agni.util.Par
import cats.{ Apply, MonadError }
import cats.instances.stream._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.option._
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql._

import scala.collection.JavaConverters._

object Cql {

  def prepareAsync[F[_]](session: CqlSession, stmt: String)(
    implicit
    par: Par.Aux[CompletionStage, F]
  ): F[PreparedStatement] = par.parallel(session.prepareAsync(stmt))

  def prepareAsync[F[_]](session: CqlSession, stmt: SimpleStatement)(
    implicit
    par: Par.Aux[CompletionStage, F]
  ): F[PreparedStatement] = par.parallel(session.prepareAsync(stmt))

  def executeAsync[F[_]](session: CqlSession, stmt: BoundStatement)(
    implicit
    par: Par.Aux[CompletionStage, F]
  ): F[AsyncResultSet] =
    par.parallel(session.executeAsync(stmt))

  def getRow[F[_]: Apply](session: CqlSession, stmt: BoundStatement)(
    implicit
    par: Par.Aux[CompletionStage, F]
  ): F[Option[Row]] =
    executeAsync(session, stmt).map(a => Option(a.one()))

  def getRowAs[F[_], A](session: CqlSession, stmt: BoundStatement)(
    implicit
    F: MonadError[F, Throwable],
    decodeA: RowDecoder[A],
    parF: Par.Aux[CompletionStage, F]
  ): F[Option[A]] =
    getRow[F](session, stmt) >>= {
      case Some(row) =>
        F.fromEither(decodeA(row, session.getContext().getProtocolVersion()).map(Option.apply))
      case _ =>
        none.pure[F]
    }

  def getRows[F[_]: MonadError[*[*], Throwable]](session: CqlSession, stmt: BoundStatement)(
    implicit
    par: Par.Aux[CompletionStage, F]
  ): F[Stream[Row]] = {
    def go(result: F[AsyncResultSet]): F[Stream[Row]] =
      result >>= { i =>
        val page = i.currentPage().asScala.toStream
        if (!i.hasMorePages) page.pure[F]
        else go(par.parallel(i.fetchNextPage())).map(page ++ _)
      }

    go(executeAsync(session, stmt))
  }

  def getRowsAs[F[_], A](session: CqlSession, stmt: BoundStatement)(
    implicit
    F: MonadError[F, Throwable],
    decodeA: RowDecoder[A],
    parF: Par.Aux[CompletionStage, F]
  ): F[Stream[A]] = {
    val ver = session.getContext().getProtocolVersion()
    getRows[F](session, stmt) >>= (_.traverse(row => F.fromEither(decodeA(row, ver))))
  }
}
