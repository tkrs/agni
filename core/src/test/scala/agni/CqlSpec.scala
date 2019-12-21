package agni

import java.util.concurrent.CompletableFuture

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{AsyncResultSet, BoundStatement, Row}
import org.scalatest._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.collection.JavaConverters._

class CqlSpec extends AsyncFunSpec with MockitoSugar with Matchers {

  import agni.std.async._
  import cats.instances.future._

  describe("getRows") {
    it("should convert to Stream[Row] the value got from AsyncResultSet#currentPage()") {
      val session = mock[CqlSession]
      val stmt = mock[BoundStatement]
      val asyncResultSet0 = mock[AsyncResultSet]

      when(session.executeAsync(stmt))
        .thenReturn(CompletableFuture.completedFuture(asyncResultSet0))

      val asyncResultSet1 = mock[AsyncResultSet]
      val asyncResultSet2 = mock[AsyncResultSet]
      val asyncResultSet3 = mock[AsyncResultSet]

      when(asyncResultSet0.fetchNextPage()).thenReturn(CompletableFuture.completedFuture(asyncResultSet1))
      when(asyncResultSet0.hasMorePages).thenReturn(true)

      when(asyncResultSet1.fetchNextPage()).thenReturn(CompletableFuture.completedFuture(asyncResultSet2))
      when(asyncResultSet1.hasMorePages).thenReturn(true)

      when(asyncResultSet2.fetchNextPage()).thenReturn(CompletableFuture.completedFuture(asyncResultSet3))
      when(asyncResultSet2.hasMorePages).thenReturn(true)

      when(asyncResultSet3.hasMorePages).thenReturn(false)

      val rows0 = Iterator.continually(mock[Row]).take(2).toIterable
      val rows1 = Iterator.continually(mock[Row]).take(2).toIterable
      val rows2 = Iterator.continually(mock[Row]).take(2).toIterable
      val rows3 = Iterator.continually(mock[Row]).take(1).toIterable

      when(asyncResultSet0.currentPage()).thenReturn(rows0.asJava)
      when(asyncResultSet1.currentPage()).thenReturn(rows1.asJava)
      when(asyncResultSet2.currentPage()).thenReturn(rows2.asJava)
      when(asyncResultSet3.currentPage()).thenReturn(rows3.asJava)

      val got = Cql.getRows[Future](session, stmt)

      got.map { xs =>
        xs shouldBe (rows0 ++ rows1 ++ rows2 ++ rows3).toStream
      }
    }

    it("should be stack-safe") {
      val session = mock[CqlSession]
      val stmt = mock[BoundStatement]
      val r = mock[AsyncResultSet]

      when(session.executeAsync(stmt)).thenReturn(CompletableFuture.completedFuture(r))

      @tailrec def createResultSet(current: AsyncResultSet, size: Int): Unit = {
        val page = Iterator.continually(mock[Row]).take(1).toIterable
        when(current.currentPage()).thenReturn(page.asJava)
        val next = mock[AsyncResultSet]
        if (size > 0) {
          when(current.hasMorePages).thenReturn(true)
          when(current.fetchNextPage()).thenReturn(CompletableFuture.completedFuture(next))
          createResultSet(next, size - 1)
        } else {
          when(current.hasMorePages).thenReturn(false)
        }
      }

      createResultSet(r, 9999)

      val got = Cql.getRows[Future](session, stmt)

      got.map { _.toList.size shouldBe 10000 }
    }
  }
}
