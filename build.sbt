import sbt._
import Keys._

lazy val slickjdbcextension = (project in file(".")).
  settings(
    name := "slick-jdbc-extension",
    organization := "com.github.tarao",
    version := "0.0.4",
    scalaVersion := "2.11.7",

    // Depenency
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.typesafe.slick" %% "slick" % "3.1.0-RC1",
      "com.github.tarao" %% "nonempty" % "0.0.2",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
      "com.h2database" % "h2" % "1.4.187" % "test"
    ),

    // Compilation
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    // Documentation
    scalacOptions in (Compile, doc) ++= Nil :::
      "-groups" ::
      "-sourcepath" ::
      baseDirectory.value.getAbsolutePath ::
      "-doc-source-url" ::
      "https://github.com/tarao/slick-jdbc-extension-scala/tree/master€{FILE_PATH}.scala" ::
      Nil,

    licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/mit-license.html")),

    // Publishing
    // publishMavenStyle := true,
    // publishTo := {
    //   val nexus = "https://oss.sonatype.org/"
    //   if (isSnapshot.value)
    //     Some("snapshots" at nexus + "content/repositories/snapshots")
    //   else
    //     Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    // },
    publishArtifact in Test := false,
    publishMavenStyle := false,
    bintrayOrganization := Some("timothyklim")
    // pomIncludeRepository := { _ => false },
    // pomExtra := (
    //   <url>https://github.com/tarao/slick-jdbc-extension-scala</url>
    //   <licenses>
    //     <license>
    //       <name>MIT License</name>
    //       <url>http://www.opensource.org/licenses/mit-license.php</url>
    //       <distribution>repo</distribution>
    //     </license>
    //   </licenses>
    //   <scm>
    //     <url>git@github.com:tarao/slick-jdbc-extension-scala.git</url>
    //     <connection>scm:git:git@github.com:tarao/slick-jdbc-extension-scala.git</connection>
    //   </scm>
    //   <developers>
    //     <developer>
    //       <id>tarao</id>
    //       <name>INA Lintaro</name>
    //       <url>https://github.com/tarao/</url>
    //     </developer>
    //   </developers>)
  )
