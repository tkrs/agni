package agni.internal

import java.util.concurrent.CompletionStage

import agni.{Cql, RowDecoder}
import agni.util.Par
import cats.MonadError
import cats.instances.lazyList._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql._

import scala.jdk.CollectionConverters._

trait Pageable { self: Cql.type =>

  def getRows[F[_]: MonadError[*[*], Throwable]](session: CqlSession, stmt: BoundStatement)(
    implicit
    par: Par.Aux[CompletionStage, F]
  ): F[LazyList[Row]] = {
    def go(result: F[AsyncResultSet]): F[LazyList[Row]] =
      result >>= { i =>
        val page = LazyList.concat(i.currentPage().asScala)
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
  ): F[LazyList[A]] = {
    val ver = session.getContext().getProtocolVersion()
    getRows[F](session, stmt) >>= (_.traverse(row => F.fromEither(decodeA(row, ver))))
  }
}
