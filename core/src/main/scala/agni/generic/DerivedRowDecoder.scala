package agni.generic

import agni.{RowDecoder, RowDeserializer}
import shapeless.labelled._
import shapeless.{::, HList, HNil, LabelledGeneric, Lazy, Witness}

trait DerivedRowDecoder[A] extends RowDecoder[A]

object DerivedRowDecoder extends DerivedRowDecoder1

trait DerivedRowDecoder1 {

  implicit val decodeHNil: DerivedRowDecoder[HNil] =
    (_, _) => Right(HNil)

  implicit def decodeLabelledHList[K <: Symbol, H, T <: HList](
    implicit
    K: Witness.Aux[K],
    H: RowDeserializer[H],
    T: DerivedRowDecoder[T]
  ): DerivedRowDecoder[FieldType[K, H] :: T] =
    (row, version) =>
      for {
        h <- H(row, K.value.name, version)
        t <- T(row, version)
      } yield field[K](h) :: t

  implicit def decodeCaseClass[A, R <: HList](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[DerivedRowDecoder[R]]
  ): DerivedRowDecoder[A] =
    (row, version) => decode.value(row, version).map(gen from)
}
