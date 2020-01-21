package agni.twitterUtil

import java.util.concurrent.{CancellationException, CompletableFuture, CompletionStage}
import java.util.function.Supplier

import agni.util.Par
import com.twitter.util.{Await, Future}
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.duration._

class AsyncSpec extends AnyFlatSpec {
  import async._

  val parF: Par.Aux[CompletionStage, Future] = implicitly

  it should "convert to a Future" in {
    val f = parF.parallel(CompletableFuture.completedStage(10))
    assert(Await.result(f) === 10)
  }

  it should "convert to a failed Future when the passed computation fails" in {
    class R extends Throwable
    val f = parF.parallel(CompletableFuture.failedStage(new R))
    assertThrows[R](Await.result(f))
  }

  it should "convert to a cancelable Future when CompletableFuture is passed" in {
    val never = CompletableFuture.supplyAsync(new Supplier[Int] {
      def get(): Int = {
        SECONDS.sleep(Int.MaxValue)
        10
      }
    })
    val f = parF.parallel(never)
    f.raise(new CancellationException)
    assertThrows[CancellationException](Await.result(f))
    assert(never.isCancelled)
  }
}
