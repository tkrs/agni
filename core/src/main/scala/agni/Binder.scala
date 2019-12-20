package agni

import com.datastax.oss.driver.api.core.ProtocolVersion
import com.datastax.oss.driver.api.core.cql.BoundStatement

trait Binder[A] {
  def apply(bound: BoundStatement, version: ProtocolVersion, a: A): Either[Throwable, BoundStatement]
}

object Binder extends TupleBinder {

  def apply[A](implicit A: Binder[A]): Binder[A] = A

  implicit def bindSingle[A](implicit serializeA: RowSerializer[A]): Binder[A] =
    (bound, version, a) => serializeA(bound, 0, a, version)
}