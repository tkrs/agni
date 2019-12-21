package agni.generic

import agni.{Binder, RowSerializer}
import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

trait DerivedBinder[A] extends Binder[A]

object DerivedBinder extends DerivedBinder1

trait DerivedBinder1 {

  implicit val bindHnil: DerivedBinder[HNil] = (bound, _, _) => Right(bound)

  implicit def bindLabelledHList[K <: Symbol, H, T <: HList](
    implicit
    K: Witness.Aux[K],
    H: RowSerializer[H],
    T: DerivedBinder[T]
  ): DerivedBinder[FieldType[K, H] :: T] =
    (bound, version, xs) => H(bound, K.value.name, xs.head, version).flatMap(T(_, version, xs.tail))

  implicit def bindCaseClass[A, R <: HList](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    bind: Lazy[DerivedBinder[R]]
  ): DerivedBinder[A] =
    (bound, version, a) => bind.value(bound, version, gen.to(a))
}
