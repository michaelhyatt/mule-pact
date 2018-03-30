package client;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mule.munit.runner.functional.FunctionalMunitSuite;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.ConsumerPactRunnerKt;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.RequestResponsePact;

public class RunClientTestToGeneratePactV2 extends FunctionalMunitSuite {

	private static final String TEST_SUITE_MUNIT_FILE = "client-test-suite.xml";
	private static final String TEST_FLOW_NAME = "client-test-suite-clientCallTest";
	private static final int PORT = 1234;
	private static final String LOCALHOST = "localhost";

	@Override
	protected String getConfigResources() {
		
		System.setProperty("http.host", LOCALHOST);
		System.setProperty("http.port", String.valueOf(PORT));
				
		return TEST_SUITE_MUNIT_FILE;
	}

	@Test
	public void test() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", "application/json");
		
		RequestResponsePact pact = ConsumerPactBuilder
				.consumer("Some Consumer")
				.hasPactWith("Some Provider")
				.uponReceiving("a request to say Hello")
					.path("/")
					.method("GET")
				.willRespondWith().status(200)
					.body("{\"result\":\"success\"}")
					.headers(headers)
				.toPact();

		MockProviderConfig config = MockProviderConfig.createDefault();
		config.setHostname(LOCALHOST);
		config.setPort(PORT);
		
		PactVerificationResult result = ConsumerPactRunnerKt.runConsumerTest(pact, config, 
				m -> runFlow(TEST_FLOW_NAME, testEvent("")));

		if (result instanceof PactVerificationResult.Error) {
			throw new RuntimeException(((PactVerificationResult.Error) result).getError());
		}

		assertEquals(PactVerificationResult.Ok.INSTANCE, result);
	}

}
