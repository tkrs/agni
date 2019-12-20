package agni.internal

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

private[agni] object ScalaVersionSpecifics {
  private[agni] type Factory[-E, +T] = CanBuildFrom[Nothing, E, T]

  implicit private[agni] class FactoryOps[F, E, T](val bf: Factory[E, T]) extends AnyVal {
    def newBuilder: mutable.Builder[E, T] = bf.apply()
  }
}
