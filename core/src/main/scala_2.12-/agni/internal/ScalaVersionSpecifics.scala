package agni.internal

import scala.collection.generic._
import scala.collection.{ mutable, GenTraversableOnce }

private[agni] object ScalaVersionSpecifics {
  private[agni] type Factory[-E, +T] = CanBuildFrom[Nothing, E, T]

  private[agni] implicit class FactoryOps[F, E, T](val underlying: Factory[E, T]) extends AnyVal {
    def newBuilder: mutable.Builder[E, T] = underlying.apply()
  }

  object IsIterableOnce {
    type Aux[Repr, A0] = IsTraversableOnce[Repr] { type A = A0 }
  }

  private[agni] implicit class IsIterableOnceOps[Repr, A](val underlying: IsIterableOnce.Aux[Repr, A]) extends AnyVal {
    def apply(coll: Repr): GenTraversableOnce[A] = underlying.conversion(coll)
  }
  private[agni] implicit class GenTraversableOnceOps[A](val underlying: GenTraversableOnce[A]) extends AnyVal {
    def iterator: Iterator[A] = underlying.toIterator
  }
}
