package agni

import com.datastax.oss.driver.api.core.ProtocolVersion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class SerializerSpec extends AnyFunSpec with Matchers {

  describe("contramap") {

    it("should return the value which applied the function") {
      val int = 123

      val x = for {
        v <- Serializer[String].contramap[Int](a => a.toString).apply(int, ProtocolVersion.DEFAULT)
        r <- Deserializer[String].apply(v, ProtocolVersion.DEFAULT)
      } yield r

      x match {
        case Left(e)  => fail(e)
        case Right(v) => assert(int.toString === v)
      }
    }
  }
}
