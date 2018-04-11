package client;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonArray;
import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;

import java.util.Date;
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
	private static final String TEST_FLOW_NAME1 = "getTest";
	private static final String TEST_FLOW_NAME2 = "postTest";
	private static final int PORT = 1234;
	private static final String LOCALHOST = "localhost";
	
	@Rule
    public PactProviderRuleMk2 provider = new PactProviderRuleMk2(PROVIDER, LOCALHOST, PORT, this);

	@Override
	protected String getConfigResources() {
		
		// Point Mule HTTP config parameters to localhost and available port 
		// for mocking:
		// <http:request-config name="HTTP_Request_Configuration"
		// host="${http.host}" port="${http.port}" basePath="/api"
		// ...
		// </http:request-config>
		System.setProperty(HTTP_HOST_PROPERTY_NAME, LOCALHOST);
		System.setProperty(HTTP_PORT_PROPERTY_NAME, String.valueOf(PORT));
				
		return TEST_SUITE_MUNIT_FILE;
	}
	
    @Pact(provider = PROVIDER, consumer = CONSUMER)
    public RequestResponsePact configurationFragment(PactDslWithProvider builder) {
    	
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json; charset=UTF-8");   	
 
        // Contract definition
        return builder
                .given("set list of contacts for retrieval")
                .uponReceiving("retrieve data from Service")
					.path("/api/contacts")
					.method("GET")
				.willRespondWith()
					.status(200)
					.headers(headers)
					.body(
							newJsonArray((a) ->
								a.object((o) -> {
									o.stringValue("first_name", "test1");
									o.stringValue("last_name", "test12");
									o.stringValue("email", "test@gmail.com");
								})
						).build())
	            .given("About to create a contact")
				.uponReceiving("creating a new entry in service")
					.matchPath("/api/contacts/[0-9]+")
					.method("POST")
					.body(
							newJsonBody((o) -> {
								o.stringType("first_name", "example1");
								o.stringType("last_name", "example2");
								o.stringType("email", "example@test.com");
								o.timestamp("creation_datetime", "yyyy-MM-dd'T'HH:mm", new Date());
							}).build()
						)
				.willRespondWith()
					.status(200)
					.body("")
				.toPact();
    }
    
    @PactVerification(PROVIDER)
    @Test
    public void testFlows() throws Exception {
    	
    	// Run MUnit flows
		runFlow(TEST_FLOW_NAME1, testEvent(""));
		runFlow(TEST_FLOW_NAME2, testEvent(""));
    }
}
