import sbt._

object Dependencies {
  val V = new {
    val `scala2.12` = "2.12.10"
    val `scala2.13` = "2.13.1"

    val kindeProjector = "0.11.0"

    val datastax   = "4.3.0"
    val cats       = "2.0.0"
    val shapeless  = "2.3.3"
    val catbird    = "19.9.0"
    val monix      = "3.1.0"
    val catsEffect = "2.0.0"
    val slf4j      = "1.7.13"

    val scalatest = "3.1.0"
    val scalatestplus = new {
      val scalacheck = "3.1.0.1"
      val mockito    = "3.1.0.0"
    }
  }

  val P = new {
    val kindeProjector = "org.typelevel" % "kind-projector" % V.kindeProjector

    val datastaxJavaDriver   = "com.datastax.oss" % "java-driver-core"          % V.datastax
    val datastaxQueryBuilder = "com.datastax.oss" % "java-driver-query-builder" % V.datastax
    val catsCore             = "org.typelevel"    %% "cats-core"                % V.cats
    val shapeless            = "com.chuusai"      %% "shapeless"                % V.shapeless
    val catbird              = "io.catbird"       %% "catbird-util"             % V.catbird
    val monixEval            = "io.monix"         %% "monix-eval"               % V.monix
    val monixTail            = "io.monix"         %% "monix-tail"               % V.monix
    val catsEffect           = "org.typelevel"    %% "cats-effect"              % V.catsEffect
    val slf4jSimple          = "org.slf4j"        % "slf4j-simple"              % V.slf4j

    lazy val scalatest  = "org.scalatest"     %% "scalatest"       % V.scalatest
    lazy val scalacheck = "org.scalatestplus" %% "scalacheck-1-14" % V.scalatestplus.scalacheck
    lazy val mockito    = "org.scalatestplus" %% "mockito-3-2"     % V.scalatestplus.mockito
  }
}
