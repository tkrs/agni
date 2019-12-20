package agni.internal

import java.util.concurrent.CompletionStage

import agni.{ Cql, RowDecoder }
import agni.util.Par
import cats.MonadError
import cats.instances.stream._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql._

import scala.collection.JavaConverters._

trait Pageable { self: Cql.type =>

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