scalacOptions ++= Seq("-unchecked", "-deprecation")
ivyLoggingLevel := UpdateLogging.Quiet

addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.0")
//addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.3")
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")
addSbtPlugin("io.spray"            % "sbt-revolver"    % "0.9.1")
addSbtPlugin("com.geirsson"        % "sbt-scalafmt"    % "1.6.0-RC4")
addSbtPlugin("ch.epfl.scala"       % "sbt-scalafix"    % "0.6.0-M12")
addSbtPlugin("com.cavorite"        % "sbt-avro-1-8"    % "1.1.3")
