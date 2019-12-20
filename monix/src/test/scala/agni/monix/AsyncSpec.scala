package agni.monix

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import agni.util.Par
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest._

import scala.concurrent.duration._

class AsyncSpec extends FlatSpec {
  import async._
  import Scheduler.Implicits.global

  val parF = implicitly[Par.Aux[CompletionStage, Task]]

  it should "convert to a Task" in {
    val f = parF.parallel(CompletableFuture.completedStage(10))
    assert(f.runSyncUnsafe() === 10)
  }

  it should "convert to a failed Task when the passed computation fails" in {
    class R extends Throwable
    val f = parF.parallel(CompletableFuture.failedStage(new R))
    assertThrows[R](f.runSyncUnsafe())
  }

  it should "convert to a cancelable Task when CompletableFuture is passed" in {
    val never = CompletableFuture.supplyAsync(new Supplier[Int] {
      def get(): Int = {
        SECONDS.sleep(Int.MaxValue)
        10
      }
    })
    val f = parF.parallel(never).runToFuture
    f.cancel()
    assert(never.isCancelled())
  }
}

