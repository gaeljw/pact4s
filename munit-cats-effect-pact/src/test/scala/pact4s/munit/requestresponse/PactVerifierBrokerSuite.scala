package pact4s.munit.requestresponse

import cats.effect.IO
import munit.CatsEffectSuite
import pact4s.MockProviderServer
import pact4s.munit.PactVerifier
import pact4s.provider.{ConsumerVersionSelector, ProviderInfoBuilder, PublishVerificationResults}

class PactVerifierBrokerSuite extends CatsEffectSuite with PactVerifier {
  val mock = new MockProviderServer(49154)

  override val provider: ProviderInfoBuilder =
    mock.brokerProviderInfo("Pact4sProvider", consumerVersionSelector = ConsumerVersionSelector().withMainBranch(true))

  override val munitFixtures: Seq[Fixture[_]] = Seq(
    ResourceSuiteLocalFixture(
      "Mock Provider Server",
      mock.server
    )
  )

  test("Verify pacts for provider `Pact4sProvider`") {
    IO(
      verifyPacts(
        publishVerificationResults = Some(
          PublishVerificationResults(
            providerVersion = "SNAPSHOT"
          )
        )
      )
    ) *> mock.featureXState.tryGet.assertEquals(None)
  }
}

