package agni.generic

import agni.{ Binder, TypedSuite }
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar

class DerivedBinderSpec extends TypedSuite with MockitoSugar {
  import auto._
  import TypedSuite._

  def checkType[A: Binder]: Assertion = {
    assertCompiles("Binder.apply[A]")
  }

  test("Binder[Named]")(checkType[Named])
  test("Binder[IDV]")(checkType[IDV])
}