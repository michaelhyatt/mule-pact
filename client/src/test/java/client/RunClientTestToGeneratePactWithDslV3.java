package client;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mule.munit.runner.functional.FunctionalMunitSuite;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;

public class RunClientTestToGeneratePactWithDslV3 extends FunctionalMunitSuite {
	
	private static final String HTTP_PORT_PROPERTY_NAME = "http.port";
	private static final String HTTP_HOST_PROPERTY_NAME = "http.host";
	private static final String CONSUMER = "SomeConsumer";
	private static final String PROVIDER = "SomeProvider";
	private static final String TEST_SUITE_MUNIT_FILE = "client-test-suite.xml";
	private static final String TEST_FLOW_NAME = "client-test-suite-clientCallTest";
	private static final int PORT = 1234;
	private static final String LOCALHOST = "localhost";
	
	@Rule
    public PactProviderRuleMk2 provider = new PactProviderRuleMk2(PROVIDER, LOCALHOST, PORT, this);

	@Override
	protected String getConfigResources() {
		
		System.setProperty(HTTP_HOST_PROPERTY_NAME, LOCALHOST);
		System.setProperty(HTTP_PORT_PROPERTY_NAME, String.valueOf(PORT));
				
		return TEST_SUITE_MUNIT_FILE;
	}
	
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public RequestResponsePact configurationFragment(PactDslWithProvider builder) {
    	
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", "application/json");   	
 
        return builder
                .given("john smith books a civic")
                .uponReceiving("retrieve data from Service")
					.path("/")
					.method("GET")
				.willRespondWith()
					.status(200)
					.body("{\"result\":\"success\"}")
					.headers(headers)
				.toPact();
    }
    
    @PactVerification(PROVIDER)
    @Test
    public void testFlow() throws Exception {
    	
		runFlow(TEST_FLOW_NAME, testEvent(""));
    }
}
