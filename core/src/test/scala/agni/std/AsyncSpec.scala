package agni.std

import java.util.concurrent.{ CompletableFuture, CompletionStage }

import agni.util.Par
import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class AsyncSpec extends FlatSpec {
  import async._

  val parF: Par.Aux[CompletionStage, Future] = implicitly

  it should "convert to a Future" in {
    val f = parF.parallel(CompletableFuture.completedStage(10))
    assert(10 === Await.result(f, 3.seconds))
  }

  it should "convert to a failed Future when the passed computation fails" in {
    class R extends Throwable
    val f = parF.parallel(CompletableFuture.failedStage(new R))
    assertThrows[R](Await.result(f, 3.seconds))
  }
}
