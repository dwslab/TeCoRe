name := "TeCoRe"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

resolvers += "PSL Releases" at "https://linqs-data.soe.ucsc.edu/maven/repositories/psl-releases/"
resolvers += "PSL Third-Party" at "https://linqs-data.soe.ucsc.edu/maven/repositories/psl-thirdparty/"
resolvers += "foo" at "https://scm.umiacs.umd.edu/maven/lccd/content/repositories/psl-releases"
resolvers += "bar" at "https://linqs-data.soe.ucsc.edu/maven/repositories/psl-releases/"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  javaJdbc,
  ehcache,
  javaWs,
  guice,
  "com.adrianhurt" %% "play-bootstrap" % "1.1-P25-B3",
  "org.webjars" % "bootstrap" % "3.3.7-1",
  "org.webjars" % "jquery" % "3.1.1-1",
  "org.jooq" % "jool" % "0.9.12",
  "org.codehaus.groovy" % "groovy" % "2.4.12",
  "org.linqs" % "psl-groovy" % "2.0.0",
  "org.linqs" % "psl-dataloading" % "1.0.0"
)