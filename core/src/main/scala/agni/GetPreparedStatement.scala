package agni

import com.datastax.driver.core.{ PreparedStatement, RegularStatement }

trait GetPreparedStatement {

  protected def getPrepared(session: SessionOp, stmt: RegularStatement): PreparedStatement
}
