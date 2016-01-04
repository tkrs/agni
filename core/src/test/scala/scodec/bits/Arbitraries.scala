package scodec.bits

import org.scalacheck.{ Arbitrary, Gen, Shrink }
import Arbitrary.arbitrary

import java.nio.ByteBuffer

// borrowed from scodec-bits' test code
object Arbitraries {

  val flatBytes = genBitVector(500, 7) // regular flat bit vectors
  val balancedTrees = genConcatBits(genBitVector(2000, 7)) // balanced trees of concatenations
  val splitVectors = genSplitBits(5000) // split bit vectors: b.take(m) ++ b.drop(m)
  val concatSplitVectors = genConcatBits(genSplitBits(5000)) // concatenated split bit vectors

  val chunk = Gen.oneOf(flatBytes, balancedTrees, splitVectors, concatSplitVectors)
  val bitStreams = genBitStream(chunk, Gen.choose(0, 5)) // streams of chunks

  // very deeply right nested - to check for SOE
  val hugeBitStreams = genBitStream(
    genBitVector(30, 7),
    Gen.choose(1000, 5000)
  )

  implicit val shrinkBitVector: Shrink[BitVector] =
    Shrink[BitVector] { b =>
      if (b.nonEmpty)
        Stream.iterate(b.take(b.size / 2))(b2 => b2.take(b2.size / 2)).takeWhile(_.nonEmpty) ++
          Stream(BitVector.empty)
      else Stream.empty
    }

  def genBitStream(g: Gen[BitVector], steps: Gen[Int]): Gen[BitVector] = for {
    n <- steps
    chunks <- Gen.listOfN(n, g)
  } yield BitVector.unfold(chunks)(s => s.headOption.map((_, s.tail)))

  def genBitVector(maxBytes: Int = 1024, maxAdditionalBits: Int = 7): Gen[BitVector] = for {
    byteSize <- Gen.choose(0, maxBytes)
    additionalBits <- Gen.choose(0, maxAdditionalBits)
    size = byteSize * 8 + additionalBits
    bytes <- Gen.listOfN((size + 7) / 8, Gen.choose(0, 255))
  } yield BitVector(ByteVector(bytes: _*)).take(size.toLong)

  def genSplitBits(maxSize: Long) = for {
    n <- Gen.choose(0L, maxSize)
    b <- genBitVector(15, 7)
  } yield {
    val m = if (b.nonEmpty) (n % b.size).abs else 0
    b.take(m) ++ b.drop(m)
  }

  def genConcatBits(g: Gen[BitVector]) =
    genBitVector(2000, 7).map { b =>
      b.toIndexedSeq.foldLeft(BitVector.empty)(
        (acc, high) => acc ++ BitVector.bit(high)
      )
    }

  def standardByteVectors(maxSize: Int): Gen[ByteVector] = for {
    size <- Gen.choose(0, maxSize)
    bytes <- Gen.listOfN(size, Gen.choose(0, 255))
  } yield ByteVector(bytes: _*)

  val sliceByteVectors: Gen[ByteVector] = for {
    bytes <- arbitrary[Array[Byte]]
    toDrop <- Gen.choose(0, bytes.size)
  } yield ByteVector.view(bytes).drop(toDrop.toInt)

  def genSplitBytes(g: Gen[ByteVector]) = for {
    b <- g
    n <- Gen.choose(0, b.size + 1)
  } yield {
    b.take(n) ++ b.drop(n)
  }

  def genByteBufferVectors(maxSize: Int): Gen[ByteVector] = for {
    size <- Gen.choose(0, maxSize)
    bytes <- Gen.listOfN(size, Gen.choose(0, 255))
  } yield ByteVector.view(ByteBuffer.wrap(bytes.map(_.toByte).toArray))

  def genConcatBytes(g: Gen[ByteVector]) =
    g.map { b => b.toIndexedSeq.foldLeft(ByteVector.empty)(_ :+ _) }

  val genVeryLargeByteVectors: Gen[ByteVector] = for {
    b <- Gen.choose(0, 255)
  } yield ByteVector.fill(Int.MaxValue.toInt + 1)(b)

  val byteVectors: Gen[ByteVector] = Gen.oneOf(
    standardByteVectors(100),
    genConcatBytes(standardByteVectors(100)),
    sliceByteVectors,
    genSplitBytes(sliceByteVectors),
    genSplitBytes(genConcatBytes(standardByteVectors(500))),
    genByteBufferVectors(100)
  )

  val bytesWithIndex = for {
    b <- byteVectors
    i <- Gen.choose(0L, (b.size + 1).toLong)
  } yield (b, i)

  implicit val arbitraryByteVectors: Arbitrary[ByteVector] = Arbitrary(byteVectors)

  implicit val shrinkByteVector: Shrink[ByteVector] =
    Shrink[ByteVector] { b =>
      if (b.nonEmpty)
        Stream.iterate(b.take(b.size / 2))(b2 => b2.take(b2.size / 2)).takeWhile(_.nonEmpty) ++ Stream(ByteVector.empty)
      else Stream.empty
    }
}
