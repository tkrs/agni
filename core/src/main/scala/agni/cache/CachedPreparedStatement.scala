package agni
package cache

import com.datastax.driver.core.{ PreparedStatement, RegularStatement }

trait CachedPreparedStatement[F[_, _]] extends GetPreparedStatement {

  protected val cache: F[String, PreparedStatement]

  override def getPrepared(ctx: SessionOp, stmt: RegularStatement): PreparedStatement

  protected def clear(): Unit
}
