package agni.catsEffect

import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.BiConsumer

import agni.util.Par
import cats.effect.ConcurrentEffect
import cats.~>

object async {
  implicit def catsConcurrentEffectPar[F[_]](implicit F: ConcurrentEffect[F]): Par.Aux[CompletionStage, F] =
    new Par[CompletionStage] {
      type G[A] = F[A]

      def parallel: CompletionStage ~> G = λ[CompletionStage ~> G](run(_))

      private[this] def run[A](fa: CompletionStage[A]): G[A] = {
        def f(cb: Either[Throwable, A] => Unit) =
          fa.whenComplete(new BiConsumer[A, Throwable] {
            def accept(t: A, u: Throwable): Unit =
              if (u != null) cb(Left(u)) else cb(Right(t))
          })

        fa match {
          case faa: CompletableFuture[_] =>
            F.cancelable { cb =>
              f(cb)
              F.delay(faa.cancel(false))
            }
          case _ =>
            F.async(f)
        }
      }
    }
}
