package agni.twitterUtil

import java.util.concurrent.{ CompletableFuture, CompletionStage }
import java.util.function.BiConsumer

import agni.util.Par
import cats.~>
import com.twitter.util.{ Future, Promise }

import scala.util.control.NonFatal

object async {
  implicit val twitterUtilPar: Par.Aux[CompletionStage, Future] = new Par[CompletionStage] {
    type G[A] = Future[A]

    def parallel: CompletionStage ~> G = λ[CompletionStage ~> G](run(_))

    private[this] def run[A](fa: CompletionStage[A]): G[A] = {
      val p = Promise[A]

      fa match {
        case faa: CompletableFuture[A] =>
          p.setInterruptHandler { case NonFatal(_) => faa.cancel(false) }
        case _ => ()
      }

      fa.whenComplete(new BiConsumer[A, Throwable] {
        def accept(t: A, u: Throwable): Unit =
          if (u != null) p.setException(u) else p.setValue(t)
      })

      p
    }
  }
}
