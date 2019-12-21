package agni.generic

import agni.{Binder, RowDecoder}
import shapeless.Lazy

object auto {
  implicit def autoDerivedBinder[A](implicit A: Lazy[DerivedBinder[A]]): Binder[A] = A.value
  implicit def autoDerivedRowDecoder[A](implicit A: Lazy[DerivedRowDecoder[A]]): RowDecoder[A] = A.value
}
