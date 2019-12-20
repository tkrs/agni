package agni.catsEffect

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import agni.util.Par
import cats.effect.{ ContextShift, IO }
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class AsyncSpec extends FlatSpec {
  import async._

  implicit def contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val parF: Par.Aux[CompletionStage, IO] = implicitly

  it should "convert to a IO" in {
    val f = parF.parallel(CompletableFuture.completedStage(10))
    assert(f.unsafeRunSync() === 10)
  }

  it should "convert to a failed IO when the passed computation fails" in {
    class R extends Throwable
    val f = parF.parallel(CompletableFuture.failedStage(new R))
    assertThrows[R](f.unsafeRunSync())
  }

  it should "convert to a cancelable IO when CompletableFuture is passed" in {
    val never = CompletableFuture.supplyAsync(new Supplier[Int] {
      def get(): Int = {
        SECONDS.sleep(Int.MaxValue)
        10
      }
    })
    parF.parallel(never).start.flatMap(_.cancel).unsafeRunSync()
    assert(never.isCancelled())
  }
}
