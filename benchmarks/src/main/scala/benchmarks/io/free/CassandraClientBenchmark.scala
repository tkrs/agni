package benchmarks.io.free

import java.util.UUID
import java.util.concurrent.{ Executor, Executors }

import agni.Get
import agni.free.session._
import benchmarks.io.DefaultSettings
import cats.data.Kleisli
import cats.free.Free
import cats.{ MonadError, ~> }
import com.datastax.driver.core.Session
import com.twitter.util.{ Await => TAwait, Future => TFuture }
import fs2.{ Strategy, Task => FTask }
import monix.eval.{ Task => MTask }
import monix.execution.Scheduler
import org.openjdk.jmh.annotations._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }

final case class Env(session: Session)

abstract class CassandraClientBenchmark[F[_]] extends DefaultSettings {

  lazy val env: Env = Env(session)

  type G[A] = Kleisli[F, Env, A]

  implicit lazy val getSession: Env => Session = _.session

  implicit def cacheableF: Cacheable[F]

  lazy val sessionOpHandler: SessionOp ~> G =
    SessionOp.Handler.sessionOpHandlerWithJ[F, Env]

  lazy val S: SessionOps[SessionOp] = implicitly

  def getUser[A: Get](uuid: UUID): Free[SessionOp, A] = for {
    p <- S.prepare(selectUser)
    b <- S.bind(p, uuid)
    l <- S.executeAsync[A](b)
  } yield l

  def interpret[A: Get](program: Free[SessionOp, A])(implicit M: MonadError[F, Throwable]): G[A] =
    program.foldMap(sessionOpHandler)

  def run[A: Get](uuid: UUID)(implicit M: MonadError[F, Throwable]): F[A] =
    interpret[A](getUser[A](uuid)).run(env)
}

@State(Scope.Benchmark)
class StdFutureBenchmark extends CassandraClientBenchmark[Future] {
  import cats.instances.future._

  implicit val context: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newWorkStealingPool())

  implicit val cacheableF: Cacheable[Future] = new agni.std.Future {}

  @Benchmark
  def one: Option[User] =
    Await.result(run[Option[User]](uuid1), Duration.Inf)
}

@State(Scope.Benchmark)
class TwitterFutureBenchmark extends CassandraClientBenchmark[TFuture] {
  import io.catbird.util.twitterFutureInstance

  implicit val executor: Executor = Executors.newWorkStealingPool()

  implicit val cacheableF: Cacheable[TFuture] = new agni.twitter.util.Future {}

  @Benchmark
  def one: Option[User] =
    TAwait.result(run[Option[User]](uuid1))
}

@State(Scope.Benchmark)
class MonixTaskBenchmark extends CassandraClientBenchmark[MTask] {
  import agni.monix.cats.taskToMonadError

  implicit val scheduler: Scheduler =
    Scheduler.computation()

  implicit val cacheableF: Cacheable[MTask] = new agni.monix.Task {}

  @Benchmark
  def one: Option[User] =
    Await.result(run[Option[User]](uuid1).runAsync, Duration.Inf)
}

@State(Scope.Benchmark)
class FS2TaskBenchmarkF extends CassandraClientBenchmark[FTask] {
  import fs2.interop.cats._

  implicit val strategy: Strategy =
    Strategy.fromExecutor(Executors.newWorkStealingPool())

  implicit val cacheableF: Cacheable[FTask] = new agni.fs2.Task {}

  @Benchmark
  def one: Option[User] =
    Await.result(run[Option[User]](uuid1).unsafeRunAsyncFuture(), Duration.Inf)
}
