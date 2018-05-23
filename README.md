# mule-pact
Consumer Driven Contracts testing with Mule.

## Summary
This is an example of how to use consumer driven contracts and Pacts to generate contracts and test APIs built in Mule. Pact website gives a very good overview of contracts, benefits of consumer driven contract testing, and how they help eliminate the need for fragile and expensive integration testing of APIs. This repo contains two Mule projecs: a client with JUnit and MUnit tests used to generate the pact, and server-side API that can be tested using the defined contracts. The contracts can be generated and validated using any of the frameworks that Pact supports.

## Generating pacts with Mule
### Write MUnit tests that will leverage mocks
Creating pacts lends itself well to Test-Driven development. First step is to define MUnit tests to individually exercise the methods and resources that will be outlined in the contract. For example, these MUnit tests call an API end point, and exercise GET and POST methods client-test-suite.xml:
```xml
    <munit:config name="munit" doc:name="MUnit configuration"/>
    <spring:beans>
        <spring:import resource="classpath:client.xml"/>
    </spring:beans>
    <munit:test name="getTest" description="Test">
        <flow-ref name="getCall" doc:name="Flow-ref to clientCall"/>
    </munit:test>
    <munit:test name="postTest" description="MUnit Test">
        <flow-ref name="postCall" doc:name="Flow Reference"/>
    </munit:test>
```
The tests above invoke the following sub-flows in client.xml:
```xml
	<flow name="getCall">
		<http:request config-ref="HTTP_Request_Configuration"
			path="/contacts" method="GET" doc:name="HTTP" />
...
	<flow name="postCall">
...
		<http:request config-ref="HTTP_Request_Configuration"
			path="/contacts/{id}" method="POST" doc:name="HTTP">
			<http:request-builder>
				<http:uri-param paramName="id" value="123123123" />
			</http:request-builder>
		</http:request>
	</flow>
```
Write JUnit tests to generate contracts
Next step is to write a JUnit test that invokes the MUnit tests, while defining/generating the contract, and supporting the MUnit tests with mock services returning responses. Here is the full example RunClientTestToGeneratePactWithDslV3.java:
```java
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
```
### Generating pacts
The pacts can be generated by running the JUnit tests written according to pact-jvm-consumer-junit. The contract format above leverages pact-jvm-consumer-java8, but the original DSL can be used as well.

### Testing APIs with pacts
Testing the API with generated contract is done using Pact maven plugin pact-jvm-provider-maven. In the sample server-side project, the configuration in pom.xml contains the following configuration:
```xml
<plugin>
   <groupId>au.com.dius</groupId>
   <artifactId>pact-jvm-provider-maven_2.11</artifactId>
   <version>3.5.11</version>
   <configuration>
      <serviceProviders>
         <serviceProvider>
            <name>SomeProvider</name>
            <protocol>http</protocol>
            <host>localhost</host>
            <port>8081</port>   
            <path>/</path>      
            <consumers>         
               <consumer>               
                  <name>SomeConsumer</name>     
                  <pactFile>src/test/resources/SomeConsumer-SomeProvider.json</pactFile>
               </consumer>              
            </consumers>        
         </serviceProvider>
      </serviceProviders>
   </configuration>
</plugin>
```
`mvn pact:verify` will run tests against locally stored pact file, but in the long run, it is recommended to use Pact broker

## TODO
* Testing and with Mule 4
* Messaging contracts example
