package agni.monix

import java.util.concurrent.{ CompletableFuture, CompletionStage }
import java.util.function.BiConsumer

import agni.util.Par
import cats.~>
import monix.eval.Task

object async {
  implicit def monixPar: Par.Aux[CompletionStage, Task] = new Par[CompletionStage] {
    type G[A] = Task[A]

    def parallel: CompletionStage ~> G = λ[CompletionStage ~> G](run(_))

    private[this] def run[A](fa: CompletionStage[A]): G[A] = {
      def f(callback: Either[Throwable, A] => Unit) =
        fa.whenComplete(new BiConsumer[A, Throwable] {
          def accept(t: A, u: Throwable): Unit = {
            if (u != null) callback(Left(u))
            else callback(Right(t))
          }
        })

      fa match {
        case faa: CompletableFuture[A] =>
          Task.cancelable { cb =>
            f(cb)
            Task(faa.cancel(false))
          }
        case _ =>
          Task.async(f)
      }
    }
  }
}
