package agni

import com.datastax.driver.core.{ PreparedStatement, Statement, BatchStatement }
import scodec.bits.ByteVector

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

object Agni extends Functions {

  def lift[A](a: A)(
    implicit
    ctx: ExecutionContext
  ): Action[Future, A] =
    withSession { session =>
      Future(a)
    }

  def execute[A](query: String)(
    implicit
    decoder: RowDecoder[A],
    ctx: ExecutionContext
  ): Action[Future, Iterator[A]] =
    withSession { session =>
      Future(session.execute(query).iterator.asScala.map(RowDecoder[A]))
    }

  def execute[A](stmt: Statement)(
    implicit
    decoder: RowDecoder[A],
    ctx: ExecutionContext
  ): Action[Future, Iterator[A]] =
    withSession { session =>
      Future(session.execute(stmt).iterator.asScala.map(RowDecoder[A]))
    }

  def batchOn(
    implicit
    ctx: ExecutionContext
  ): Action[Future, BatchStatement] =
    withSession { _ =>
      Future(new BatchStatement)
    }

  def prepare(query: String)(
    implicit
    ctx: ExecutionContext
  ): Action[Future, PreparedStatement] =
    withSession { session =>
      Future(session.prepare(query))
    }

  def bind(bstmt: BatchStatement, pstmt: PreparedStatement, ps: Any*)(
    implicit
    ctx: ExecutionContext
  ): Action[Future, Unit] =
    withSession { session =>
      Future(bstmt.add(pstmt.bind(ps.map(convertToJava): _*)))
    }

  // TODO: improve implementation
  val convertToJava: Any => Object = (any: Any) => any match {
    case a: Set[Any] => a.map(convertToJava).asJava: java.util.Set[Object]
    case a: Map[Any, Any] => a.map { case (k, v) => (convertToJava(k), convertToJava(v)) }.asJava: java.util.Map[Object, Object]
    case a: String => a
    case a: Long => a: java.lang.Long
    case a: Int => a: java.lang.Integer
    case a: Float => a: java.lang.Float
    case a: Double => a: java.lang.Double
    case a: BigDecimal => a.bigDecimal
    case a: BigInt => a.bigInteger
    case a: ByteVector => a.toArray
    case Some(a) => convertToJava(a)
    case None => null
    case a: Object => a
    case a => new RuntimeException(s"uncaught class type ${a}")
  }

}
