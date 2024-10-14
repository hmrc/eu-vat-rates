import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.5.0"
  private val hmrcMongoVersion = "2.2.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
    "org.apache.axis2" % "axis2-kernel" % "1.8.2",
    "org.apache.wss4j" % "wss4j-ws-security-dom" % "2.4.3",
    "uk.gov.hmrc"    %% "internal-auth-client-play-30"      % "3.0.0"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0",
    "org.scalacheck" %% "scalacheck" % "1.17.0",
  )

  val it = Seq.empty
}
