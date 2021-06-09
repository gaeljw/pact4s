package pact4s.scalatest

import au.com.dius.pact.provider.{MessageAndMetadata, PactVerifyProvider}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.BeforeAndAfterAll
import pact4s.{MockProviderServer, ProviderInfoBuilder, VerificationType}

import scala.jdk.CollectionConverters._

class Issue19ReproducerSuite extends PactVerifier with BeforeAndAfterAll {
  lazy val mock = new MockProviderServer(3459)

  // Because this is mutable, it will trigger the issue.
  var metadata: Map[String, String] = _

  def provider: ProviderInfoBuilder = mock.fileSourceProviderInfo(
    "Pact4sMessageConsumer",
    "Pact4sMessageProvider",
    // Because annotated methods are available across the classpath (by description), we must use a unique description
    // for the method we test here.
    "./scalatest-pact/src/test/resources/issue19_reproducer.json",
    VerificationType.AnnotatedMethod
  )

  @PactVerifyProvider("Issue 19 reproducer")
  def issue19ReproducerMessage(): MessageAndMetadata = {
    val body = Json.obj("issue" -> "19".asJson)
    new MessageAndMetadata(body.toString.getBytes, metadata.asJava)
  }

  var cleanUp: IO[Unit] = IO.unit

  override def beforeAll(): Unit = {
    val (_, shutdown) = mock.server.allocated.unsafeRunSync()
    metadata = Map("reproduced" -> "true")
    cleanUp = shutdown
  }

  override def afterAll(): Unit = cleanUp.unsafeRunSync()

  verifyPacts(
    // Issue #19
    // If the declaring class of the annotated method has mutable properties, we must ensure the verifier uses the same
    // instance is used when invoking the method, otherwise the state of the property will be wrong.
    providerMethodInstance = Some(this)
  )
}
