package agni.internal

private[agni] object ScalaVersionSpecifics {
  private[agni] type Factory[-E, +T] = scala.collection.Factory[E, T]

  object IsIterableOnce {
    type Aux[A0, B] = scala.collection.generic.IsIterableOnce[A0] { type A = B }
  }
}
