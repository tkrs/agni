package agni

import com.datastax.oss.driver.api.core.ProtocolVersion
import com.datastax.oss.driver.api.core.cql.Row

trait RowDecoder[A] {
  def apply(row: Row, version: ProtocolVersion): Either[Throwable, A]
}

object RowDecoder extends TupleRowDecoder {

  def apply[A](implicit A: RowDecoder[A]): RowDecoder[A] = A

  implicit def decodeSingleColumn[A](implicit deserializeA: RowDeserializer[A]): RowDecoder[A] =
    (row, version) => deserializeA(row, 0, version)
}
