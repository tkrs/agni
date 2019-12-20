package agni.generic

import shapeless.Lazy

object semiauto {
  def derivedBinder[A](implicit A: Lazy[DerivedBinder[A]]): DerivedBinder[A] = A.value
}