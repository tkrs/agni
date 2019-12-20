package agni.generic

import agni.{ Binder, RowDecoder }
import shapeless.Lazy

object semiauto {
  def derivedBinder[A](implicit A: Lazy[DerivedBinder[A]]): Binder[A] = A.value
  def derivedRowDecoder[A](implicit A: Lazy[DerivedRowDecoder[A]]): RowDecoder[A] = A.value
}