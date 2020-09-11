
version := "0.1"

scalaVersion := "2.12.6"

val minitest = "io.monix" %% "minitest" % "2.1.1" % "test"
val shapeless =   "com.chuusai" %% "shapeless" % "2.3.3"
val cats = "org.typelevel" %% "cats-core" % "1.1.0"


val minitestFramework = new TestFramework("minitest.runner.Framework")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

val circeVersion = "0.9.3"

val matroyshka = "com.slamdata" %% "matryoshka-core" % "0.18.3"

lazy val djala = (project in file("."))
  .aggregate(idless, sourceless, formless, endless)
  .dependsOn(endless)
  .settings(
    name := "djala",
    libraryDependencies += minitest,
    testFrameworks += minitestFramework
  )


lazy val idless = (project in file("idless"))
  .settings(
    name := "idless",
    libraryDependencies ++= Seq(
      minitest,
      shapeless,
      cats
    ),
    testFrameworks += minitestFramework
  )

lazy val sourceless = (project in file("sourceless"))
  .dependsOn(idless)
  .settings(
    name := "sourceless",
    libraryDependencies ++= Seq(minitest, matroyshka),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    testFrameworks += minitestFramework
  )

lazy val formless = (project in file("formless"))
  .dependsOn(sourceless)
  .settings(
    name := "formless"
  )

lazy val endless = (project in file("endless"))
  .dependsOn(sourceless)
  .settings(
    name := "endless"
  )

