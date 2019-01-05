val fs2Version     = "0.10.6"
val Http4sVersion  = "0.18.17"
val Specs2Version  = "4.2.0"
val LogbackVersion = "1.2.3"
val doobieVersion  = "0.5.3"

lazy val root = (project in file("."))
  .settings(
    name := "text-service",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    scalacOptions ++= Seq(
      "-language:_",
      "-Ypartial-unification",
      "-Xfatal-warnings"
    ),
    scalafmtConfig := Some(file("project/scalafmt.conf")),
    scalafixConfig := Some(file("project/scalafix.conf")),
    libraryDependencies ++= Seq(
      "com.github.mpilquist"    %% "simulacrum"          % "0.12.0",
      "org.scalaz"              %% "scalaz-core"         % "7.2.26",
      "org.scalaz"              %% "scalaz-zio"          % "0.2.8",
      "org.scalaz"              %% "scalaz-zio-interop"  % "0.2.8",
      "com.codecommit"          %% "shims"               % "1.4.0",
      "com.codecommit"          %% "shims-effect"        % "1.4.0",
      "co.fs2"                  %% "fs2-core"            % "0.10.4",
      "co.fs2"                  %% "fs2-scalaz"          % "0.3.0",
      "org.http4s"              %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"              %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"              %% "http4s-circe"        % Http4sVersion,
      "io.circe"                %% "circe-generic"       % "0.9.3",
      "io.circe"                %% "circe-literal"       % "0.9.3",
      "org.http4s"              %% "http4s-dsl"          % Http4sVersion,
      "org.http4s"              %% "http4s-scala-xml"    % Http4sVersion,
      "org.scalactic"           %% "scalactic"           % "3.0.5",
      "org.scalatest"           %% "scalatest"           % "3.0.5" % "test",
      "ch.qos.logback"          % "logback-classic"      % LogbackVersion,
      "com.twilio.sdk"          % "twilio"               % "7.23.1",
      "org.tpolecat"            %% "doobie-core"         % doobieVersion,
      "org.tpolecat"            %% "doobie-h2"           % doobieVersion,
      "org.tpolecat"            %% "doobie-scalatest"    % doobieVersion,
      "com.microsoft.sqlserver" % "mssql-jdbc"           % "6.4.0.jre8",
      "io.chrisdavenport"       %% "log4cats-slf4j"      % "0.1.1",
      "io.chrisdavenport"       %% "log4cats-noop"       % "0.1.1",
      "com.github.pureconfig"   %% "pureconfig"          % "0.9.2",
      "com.ovoenergy"           %% "fs2-kafka-client"    % "0.1.21",
      "com.mindscapehq"         % "core"                 % "3.0.0"
    )
  )

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin(
  ("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full)
)
addCompilerPlugin(
  ("org.scalameta" % "semanticdb-scalac" % "4.0.0-M6").cross(CrossVersion.full)
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "check",
  "all headerCheck test:headerCheck scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)
addCommandAlias("lint", "all compile:scalafixTest test:scalafixTest")
addCommandAlias("fix", "all compile:scalafixCli test:scalafixCli")

scalacOptions in (Compile, console) -= "-Yno-imports"
scalacOptions in (Compile, console) -= "-Yno-predef"
initialCommands in (Compile, console) := Seq(
  "scalaz._, Scalaz._",
  "shapeless._"
).mkString("import ", ",", "")

resolvers += "Sonatype OSS Snapshots".at(
  "https://oss.sonatype.org/content/repositories/snapshots"
)
resolvers += "Sonatype OSS Public".at(
  "https://oss.sonatype.org/content/repositories/public"
)
resolvers += "Ovotech".at("https://dl.bintray.com/ovotech/maven")
resolvers += "confluent".at("http://packages.confluent.io/maven/")
