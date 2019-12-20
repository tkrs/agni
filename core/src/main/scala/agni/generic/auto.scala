package agni.generic

import shapeless.Lazy
import agni.Binder

object auto {
  implicit def autoDerivedBinder[A](implicit A: Lazy[DerivedBinder[A]]): Binder[A] = A.value
}