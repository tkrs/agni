package agni

import cats.MonadError
import com.datastax.driver.core._

trait Agni[F[_], E] { self: GetPreparedStatement =>

  implicit val F: MonadError[F, E]

  def get[A: Get](query: String)(implicit s: SessionOp, ev: Throwable <:< E): F[A] =
    Get[A].apply[F, E](s.execute(new SimpleStatement(query)), s.protocolVersion)

  def get[A: Get](stmt: Statement)(implicit s: SessionOp, ev: Throwable <:< E): F[A] =
    Get[A].apply[F, E](s.execute(stmt), s.protocolVersion)

  def batchOn: F[BatchStatement] =
    F.pure(new BatchStatement)

  def prepare(q: String)(implicit s: SessionOp, ev: Throwable <:< E): F[PreparedStatement] =
    F.catchNonFatal(getPrepared(s, new SimpleStatement(q)))

  def prepare(stmt: RegularStatement)(implicit s: SessionOp, ev: Throwable <:< E): F[PreparedStatement] =
    F.catchNonFatal(getPrepared(s, stmt))

  def bind[A: Binder](pstmt: PreparedStatement, a: A)(implicit s: SessionOp, ev: Throwable <:< E): F[BoundStatement] =
    Binder[A].apply(pstmt.bind(), s.protocolVersion, a).fold(F.raiseError(_), F.pure)
}
