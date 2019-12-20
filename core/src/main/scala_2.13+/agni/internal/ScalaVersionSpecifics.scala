package agni.internal

private[agni] object ScalaVersionSpecifics {
  private[agni] type Factory[-E, +T] = scala.collection.Factory[E, T]
}
