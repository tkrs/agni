import java.time.LocalDate
import java.util.UUID

import agni.catsEffect.async._
import agni.{ Binder, RowDecoder, Cql }
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.querybuilder.{ QueryBuilder => Q }
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp with Matchers {
  import Query._

  def run(args: List[String]): IO[ExitCode] =
    IO(connect()).bracket(action)(c => IO(c.close())).flatTap(xs => IO(xs.foreach(println))) >>=
      (xs => IO(xs.sortBy(_.id) === users).ifM(IO(ExitCode.Success), IO(ExitCode.Error)))

  def connect(): CqlSession =
    CqlSession.builder().build()

  def remake(session: CqlSession): IO[Unit] = for {
    _ <- Cql.prepareAsync[IO](session, createKeyspaceQuery) >>= (p => Cql.executeAsync[IO](session, p.bind()))
    _ <- Cql.prepareAsync[IO](session, dropTableQuery) >>= (p => Cql.executeAsync[IO](session, p.bind()))
    _ <- Cql.prepareAsync[IO](session, createTableQuery) >>= (p => Cql.executeAsync[IO](session, p.bind()))
  } yield ()

  private[this] val decode: RowDecoder[Author] = RowDecoder[Author]

  def action(session: CqlSession): IO[Stream[Author]] = for {
    _ <- remake(session)

    _ <- Cql.prepareAsync[IO](session, insertUserQuery).flatTap(a => IO(println(a.getQuery))) >>=
      (p => users.traverse(x => insertUser(session, p, x)))

    v = session.getContext.getProtocolVersion

    xs <- Cql.prepareAsync[IO](session, selectUserQuery).flatTap(a => IO(println(a.getQuery))) >>=
      (p => Cql.getRows[IO](session, p.bind())) >>=
      (rows => rows.traverse(row => IO.fromEither(decode(row, v))))
  } yield xs

  def insertUser(session: CqlSession, p: PreparedStatement, a: Author): IO[Unit] =
    IO.fromEither(Binder[Author].apply(p.bind(), session.getContext.getProtocolVersion, a)).flatTap(a => IO(println(a.getUuid(0)))) >>=
      (b => Cql.executeAsync[IO](session, b) >>
        IO(println("inserted users")))

  val users = List(
    Author(UUID.randomUUID(), "Edna", "O'Brien", LocalDate.of(1932, 12, 15), "female", Map(
      "The Country Girls" -> 1960,
      "Girl with Green Eyes" -> 1962,
      "Girls in Their Married Bliss" -> 1964,
      "August is a Wicked Month" -> 1965,
      "Casualties of Peace" -> 1966,
      "Mother Ireland" -> 1976
    )),
    Author(UUID.randomUUID(), "Benedict", "Kiely", LocalDate.of(1919, 8, 15), "male", Map(
      "The Collected Stories of Benedict Kiely" -> 2001,
      "The Trout in the Turnhole" -> 1996,
      "A Letter to Peachtree" -> 1987,
      "The State of Ireland: A Novella and Seventeen Short Stories" -> 1981,
      "A Cow in the House" -> 1978,
      "A Ball of Malt and Madame Butterfly" -> 1973,
      "A Journey to the Seven Streams" -> 1963
    )),
    Author(UUID.randomUUID(), "Darren", "Shan", LocalDate.of(1972, 7, 2), "male", Map(
      "Cirque Du Freak" -> 2000,
      "The Vampire's Assistant" -> 2000,
      "Tunnels of Blood" -> 2000
    ))
  ).sortBy(_.id)

}

object Query {
  private[this] val keyspace = "agni_test"
  private[this] val tableName = "author"

  val createKeyspaceQuery =
    s"""CREATE KEYSPACE IF NOT EXISTS $keyspace
       |  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
       |""".stripMargin

  val dropTableQuery =
    s"DROP TABLE IF EXISTS $keyspace.$tableName"

  val createTableQuery =
    s"""CREATE TABLE $keyspace.$tableName (
       |  id uuid PRIMARY KEY,
       |  first_name ascii,
       |  last_name ascii,
       |  birth date,
       |  gender ascii,
       |  works map<ascii, int>,
       |)""".stripMargin

  val insertUserQuery =
    Q.insertInto(keyspace, tableName)
      .value("id", Q.bindMarker())
      .value("first_name", Q.bindMarker())
      .value("last_name", Q.bindMarker())
      .value("birth", Q.bindMarker())
      .value("gender", Q.bindMarker())
      .value("works", Q.bindMarker())
      .build()

  val selectUserQuery =
    Q.selectFrom(keyspace, tableName).all().build()
}

final case class Author(
  id: UUID,
  first_name: String,
  last_name: String,
  birth: LocalDate,
  gender: String,
  works: Map[String, Int]
)
