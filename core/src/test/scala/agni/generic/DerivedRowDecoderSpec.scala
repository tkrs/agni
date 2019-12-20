package agni.generic

import agni.{ RowDecoder, TypedSuite }
import org.scalatest.Assertion

class RowDecoderSpec extends TypedSuite {
  import TypedSuite._
  import auto._

  def checkType[A: RowDecoder]: Assertion = {
    assertCompiles("RowDecoder.apply[A]")
  }

  test("RowDecoder[Named]")(checkType[Named])
  test("RowDecoder[IDV]")(checkType[IDV])
}
