package agni
package cache

import com.datastax.driver.core.{ PreparedStatement, RegularStatement }

import scala.collection.concurrent.TrieMap

trait CachedPreparedStatementWithTrieMap extends CachedPreparedStatement[TrieMap] {

  protected val cache: TrieMap[String, PreparedStatement]

  override def getPrepared(session: SessionOp, stmt: RegularStatement): PreparedStatement =
    cache.getOrElseUpdate(stmt.toString, session.prepare(stmt))

  override def clear(): Unit = cache.clear()
}
