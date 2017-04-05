package agni
package std

import agni.cache.CachedPreparedStatementWithGuava
import cats.instances.try_._
import com.datastax.driver.core.PreparedStatement
import com.google.common.cache.Cache

import scala.util.{ Try => STry }

abstract class Try(implicit _cache: Cache[String, PreparedStatement])
    extends Agni[STry, Throwable] with CachedPreparedStatementWithGuava {

  override protected val cache: Cache[String, PreparedStatement] = _cache
}
