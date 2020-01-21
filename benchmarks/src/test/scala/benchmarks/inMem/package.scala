package benchmarks

import agni.RowDecoder
import agni.generic.semiauto._

package object inMem {
  type S5 = (String, String, String, String, String)
  type S22 = (String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String)
}

package inMem {

  final case class C5(s1: String, s2: String, s3: String, s4: String, s5: String)
  object C5 {
    implicit val e: RowDecoder[C5] = derivedRowDecoder[C5]
  }
  final case class C22(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String, s9: String, s10: String, s11: String, s12: String, s13: String, s14: String, s15: String, s16: String, s17: String, s18: String, s19: String, s20: String, s21: String, s22: String)
  object C22 {
    implicit val e: RowDecoder[C22] = derivedRowDecoder[C22]
  }
  final case class C30(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String, s7: String, s8: String, s9: String, s10: String, s11: String, s12: String, s13: String, s14: String, s15: String, s16: String, s17: String, s18: String, s19: String, s20: String, s21: String, s22: String, s23: String, s24: String, s25: String, s26: String, s27: String, s28: String, s29: String, s30: String)
  object C30 {
    implicit val e: RowDecoder[C30] = derivedRowDecoder[C30]
  }
}
