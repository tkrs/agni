package agni.std

import java.util.concurrent.CompletionStage
import java.util.function.BiConsumer

import agni.util.Par
import cats.~>

import scala.concurrent.{Future, Promise}

object async {

  implicit val scalaFuturePar: Par.Aux[CompletionStage, Future] = new Par[CompletionStage] {
    type G[A] = Future[A]

    def parallel: CompletionStage ~> G = λ[CompletionStage ~> G](run(_))

    private[this] def run[A](fa: CompletionStage[A]): G[A] = {
      val p = Promise[A]

      fa.whenComplete(new BiConsumer[A, Throwable] {
        def accept(t: A, u: Throwable): Unit =
          if (u != null) p.failure(u)
          else p.success(t)
      })

      p.future
    }
  }
}
