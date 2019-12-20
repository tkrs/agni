import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

val preferences =
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(DanglingCloseParenthesis, Force)

lazy val root = project.in(file("."))
  .settings(name := "agni")
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, `twitter-util`, monix, `cats-effect`, examples)
  .dependsOn(core, `twitter-util`, monix, `cats-effect`, examples)

lazy val allSettings = Seq.concat(
  buildSettings,
  baseSettings,
  publishSettings,
  Seq(preferences)
)

lazy val buildSettings = Seq(
  organization := "com.github.yanana",
  scalaVersion := "2.13.1",
  crossScalaVersions := Seq("2.12.10", "2.13.1"),
  libraryDependencies += compilerPlugin(("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full))
)

val datastaxVersion = "4.3.0"
val catsVersion = "2.0.0"
val shapelessVersion = "2.3.3"
val scalacheckVersion = "1.14.1"
val scalatestVersion = "3.0.8"
val catbirdVersion = "19.9.0"
val monixVersion = "3.1.0"
val catsEffectVersion = "2.0.0"
val mockitoVersion = "3.1.0"

lazy val coreDeps = Seq(
  "com.datastax.oss" % "java-driver-core" % datastaxVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "com.chuusai" %% "shapeless" % shapelessVersion
)

lazy val testDeps = Seq(
  "org.scalacheck" %% "scalacheck" % scalacheckVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion,
  "org.mockito" % "mockito-core" % mockitoVersion
) map (_ % "test")

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => Nil
      case _                       => Seq("-Xfuture","-Ypartial-unification", "-Yno-adapted-args")
    }
  },
  scalacOptions in (Compile, console) := compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions,
  libraryDependencies ++= (coreDeps ++ testDeps).map(_.withSources),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  fork in Test := true,
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused:_"))
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/yanana/agni")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/yanana/agni"),
      "scm:git:git@github.com:yanana/agni.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>yanana</id>
        <name>Shun Yanaura</name>
        <url>https://github.com/yanana</url>
      </developer>
      <developer>
        <id>tkrs</id>
        <name>Takeru Sato</name>
        <url>https://github.com/tkrs</url>
      </developer>
    </developers>,
  pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray),
  pgpSecretRing := sys.env.get("PGP_SECRET_RING").fold(pgpSecretRing.value)(file)
)

lazy val noPublishSettings = Seq(
  publish := ((): Unit),
  publishLocal := ((): Unit),
  publishArtifact := false
)

lazy val crossVersionSharedSources: Seq[Setting[_]] =
  Seq(Compile, Test).map { sc =>
    (sc / unmanagedSourceDirectories) ++= {
      (sc / unmanagedSourceDirectories).value.flatMap { dir =>
        if (dir.getName != "scala") Seq(dir)
        else
          CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((2, n)) if n >= 13 => Seq(file(dir.getPath + "_2.13+"))
            case _                       => Seq(file(dir.getPath + "_2.12-"))
          }
      }
    }
  }


lazy val core = project.in(file("core"))
  .settings(allSettings)
  .settings(crossVersionSharedSources)
  .settings(
    sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue
  )
  .settings(
    description := "agni core",
    moduleName := "agni-core",
    name := "core"
  )

lazy val `twitter-util` = project.in(file("twitter-util"))
  .settings(allSettings)
  .settings(
    description := "agni twitter-util",
    moduleName := "agni-twitter-util",
    name := "twitter-util",
  )
  .settings(
    libraryDependencies ++= Seq(
      "io.catbird" %% "catbird-util" % catbirdVersion
    )
  )
  .dependsOn(core)

lazy val monix = project.in(file("monix"))
  .settings(allSettings)
  .settings(
    description := "agni monix",
    moduleName := "agni-monix",
    name := "monix",
  )
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %% "monix-eval" % monixVersion,
      "io.monix" %% "monix-tail" % monixVersion
    )
  )
  .dependsOn(core)

lazy val `cats-effect` = project.in(file("cats-effect"))
  .settings(allSettings)
  .settings(
    description := "agni cats-effect",
    moduleName := "agni-cats-effect",
    name := "cats-effect",
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion
    )
  )
  .dependsOn(core)

lazy val benchmarks = project.in(file("benchmarks"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "agni benchmarks",
    moduleName := "agni-benchmarks",
    name := "benchmarks",
  )
  .settings(
    scalacOptions ++= Seq(
      "-opt:l:inline",
      "-opt-inline-from:**",
      "-opt-warnings"
    )
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core % "test->test")

lazy val examples = project.in(file("examples"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "agni examples",
    moduleName := "agni-examples",
    name := "examples",
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.datastax.oss" % "java-driver-query-builder" % datastaxVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.slf4j" % "slf4j-simple" % "1.7.13",
      "org.scalatest" %% "scalatest" % scalatestVersion
    )
  )
  .dependsOn(`cats-effect`)

lazy val compilerOptions = Seq(
  "-target:jvm-1.8",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused:_",
  "-Xlint"
)
