package agni

import com.datastax.driver.core._
import com.google.common.util.concurrent.ListenableFuture

trait SessionOp {

  def protocolVersion: ProtocolVersion

  //  def execute(query: String): ResultSet
  //
  //  def execute(query: String , values: Any*): ResultSet
  //
  //  def execute(query: String, values: Map[String, AnyRef]): ResultSet

  def execute(statement: Statement): ResultSet

  //  def executeAsync(query: String, values: Any*): ResultSetFuture
  //
  //  def executeAsync(query: String, values: Map[String, AnyRef]): ResultSetFuture

  def executeAsync(statement: Statement): ResultSetFuture

  //  def prepare(query: String): PreparedStatement

  def prepare(statement: RegularStatement): PreparedStatement

  //  def prepareAsync(query: String): ListenableFuture[PreparedStatement]

  def prepareAsync(statement: RegularStatement): ListenableFuture[PreparedStatement]

  def closeAsync(): CloseFuture

  def close(): Unit
}

object SessionOp {

  def apply(implicit s: Session): SessionOp = new SessionOp {

    def protocolVersion: ProtocolVersion =
      s.getCluster.getConfiguration.getProtocolOptions.getProtocolVersion

    def prepare(statement: RegularStatement): PreparedStatement = s.prepare(statement)

    def prepareAsync(statement: RegularStatement): ListenableFuture[PreparedStatement] = s.prepareAsync(statement)

    def execute(statement: Statement): ResultSet = s.execute(statement)

    def executeAsync(statement: Statement): ResultSetFuture = s.executeAsync(statement)

    def closeAsync(): CloseFuture = s.closeAsync()

    def close(): Unit = s.close()
  }
}
