import Dependencies._

lazy val agni = project
  .in(file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .aggregate(core, `twitter-util`, monix, `cats-effect`, examples)
  .dependsOn(core, `twitter-util`, monix, `cats-effect`, examples)

lazy val allSettings = Seq.concat(
  buildSettings,
  baseSettings,
  publishSettings
)

lazy val buildSettings = Seq(
  organization := "com.github.tkrs",
  scalaVersion := V.`scala2.13`,
  crossScalaVersions := Seq(V.`scala2.12`, V.`scala2.13`),
  libraryDependencies += compilerPlugin((P.kindeProjector).cross(CrossVersion.full))
)

lazy val coreDeps = Seq(P.datastaxJavaDriver, P.catsCore, P.shapeless)

lazy val testDeps = Seq(P.scalacheck, P.scalatest, P.mockito).map(_ % Test)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => Nil
      case _                       => Seq("-Xfuture", "-Ypartial-unification", "-Yno-adapted-args")
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
  scalacOptions in (Compile, console) ~= (_.filterNot(_ == "-Ywarn-unused:_"))
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/tkrs/agni")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := (_ => false),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/tkrs/agni"),
      "scm:git:git@github.com:tkrs/agni.git"
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
  skip in publish := true
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

lazy val core = project
  .in(file("core"))
  .settings(allSettings)
  .settings(crossVersionSharedSources)
  .settings(
    sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue
  )
  .settings(
    description := "agni core",
    moduleName := "agni-core"
  )

lazy val `twitter-util` = project
  .in(file("twitter-util"))
  .settings(allSettings)
  .settings(
    description := "agni twitter-util",
    moduleName := "agni-twitter-util"
  )
  .settings(
    libraryDependencies ++= Seq(P.catbird)
  )
  .dependsOn(core)

lazy val monix = project
  .in(file("monix"))
  .settings(allSettings)
  .settings(
    description := "agni monix",
    moduleName := "agni-monix"
  )
  .settings(
    libraryDependencies ++= Seq(P.monixEval, P.monixTail)
  )
  .dependsOn(core)

lazy val `cats-effect` = project
  .in(file("cats-effect"))
  .settings(allSettings)
  .settings(
    description := "agni cats-effect",
    moduleName := "agni-cats-effect"
  )
  .settings(
    libraryDependencies ++= Seq(P.catsEffect)
  )
  .dependsOn(core)

lazy val benchmarks = project
  .in(file("benchmarks"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "agni benchmarks",
    moduleName := "agni-benchmarks"
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

lazy val examples = project
  .in(file("examples"))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    description := "agni examples",
    moduleName := "agni-examples"
  )
  .settings(
    libraryDependencies ++= Seq(P.datastaxQueryBuilder, P.slf4jSimple, P.scalatest)
  )
  .dependsOn(`cats-effect`)

lazy val compilerOptions = Seq(
  "-target:jvm-1.8",
  "-deprecation",
  "-encoding",
  "UTF-8",
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
