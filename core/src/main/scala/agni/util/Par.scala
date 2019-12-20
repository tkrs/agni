package agni
package util

import cats.~>

trait Par[F[_]] {
  type G[_]

  def parallel: F ~> G
}

object Par {
  type Aux[F[_], G0[_]] = Par[F] { type G[A] = G0[A] }
}
