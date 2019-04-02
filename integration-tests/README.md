# Syndesis integration tests

This repository contains integration tests for Syndesis. The tests start integration runtimes in 
[Docker](https://www.docker.com/) (using [Testcontainers](https://www.testcontainers.org/)) and exchange messages with
the running integration. The integration outcome gets consumed and validated by simulated 3rd party services and/or within 
the database.

##### Table of Contents

* [Setup and preparations](#setup-and-preparations)
* [Test environment](#test-environment)
* [Running tests](#running-tests)
* [Syndesis integration runtime](#syndesis-integration-runtime)
  * [From integration export](#from-integration-export)
  * [From integration model](#from-integration-model)
  * [From integration fat jar](#from-integration-fat-jar)
  * [From integration project](#from-integration-project)
  * [From integration json](#from-integration-json)
* [Syndesis db container](#syndesis-db-container)
* [Syndesis server container](#syndesis-server)
* [Infrastructure containers](#infrastructure-containers)
  * [AMQ Message Broker](#amq-message-broker)
  * [Kafka Message Broker](#kafka-message-broker)
* [Simulate 3rd party interfaces](#simulate-3rd-party-interfaces)
* [Customize integrations](#customize-integrations)
* [Logging](#logging)

## Setup and preparations

The integration tests in this repository use [JUnit](https://junit.org/), [Testcontainers](https://www.testcontainers.org/) 
and [Citrus](https://citrusframework.org/) as base frameworks. 

Each integration test prepares and starts a group of Docker containers as Testcontainers. 
These containers represent Syndesis backend server functionality, Syndesis integration runtime and infrastructure components 
such as Postgres DB, Kafka or AMQ message brokers. In addition to that the tests prepares 3rd party services that get simulated with Citrus framework.

You need [Docker](https://www.docker.com/) available on your host to run the tests.

## Test environment

The integration tests usually exchange data with Syndesis integrations. Therefore the Syndesis integration runtime is the 
primary system under test.

Each test uses a specific group of required infrastructure components and builds its very specific Syndesis integration runtime 
as a Testcontainer. All required infrastructure components and the defined integration runtime are built and run automatically before the test.

Once the test infrastructure is provided with Testcontainers the test interacts with the running Syndesis integration. Usually the test invokes
the integration start connection and consumes the integration output for verification.

Each test defines the required components individually so Testcontainers are automatically started and stopped before and after the tests. All tests share
a common Postgres database container that holds the Syndesis data as well as sample database tables to test data.

### System properties / environment variables

You can influence the test environment by setting several properties or environment variables for the test runtime. You can set these 
as system properties in Maven and/or in your Java IDE or as environment variables on your host.

The following system properties (or environment variables) are known to the project

* **syndesis.version** / **SYNDESIS_VERSION**
    * Version of Syndesis used as system under test. By default this is the latest SNAPSHOT version. You can also use tagged 
    release or daily build versions as listed here: [https://github.com/syndesisio/syndesis/releases](https://github.com/syndesisio/syndesis/releases)
    Maven artifact versions are translated to Docker hub image versions (e.g. 1.7-SNAPSHOT=latest).
* **syndesis.image.tag** / **SYNDESIS_IMAGE_TAG**
    * Docker image tag to use for all Syndesis images. You can use this explicit image version when automatic version translation 
    form Maven artifact name is not working for you.
* **syndesis.s2i.build.enabled** / **SYNDESIS_S2I_BUILD_ENABLED**
    * By default the test containers use a Spring Boot build an runtime environment in the Syndesis integration runtime container. You can also use
    the S2i image to build and run the integration. The S2i image build is close to production but slower in execution.

## Running tests

You can run the tests from your favorite Java IDE (e.g. Eclipse, IntelliJ) as normal JUnit test. Also you can run all available tests with
Maven build tool:

```bash
mvn verify
```

This will execute all available integration tests. You can also run single tests or test methods. Just give the test class name and/or test method name as
an argument.

```bash
mvn verify -Dit.test=MyTestClassName
```

```bash
mvn verify -Dit.test=MyTestClassName#mytestMethodName
```

## Syndesis integration runtime

Syndesis executes integrations with a special runtime container. The container is usually provided with a generated integration project holding all sources needed to run the
integration (such as integration.json, pom.xml, atlas-mappings, application.properties, secrets and so on). The integration runtime container usually builds from the `syndesis-s2i:latest` 
Docker image that already holds all required Syndesis artifacts and required 3rd party libs.

The integration tests provide a Testcontainer that represents the runtime container. You can add the integration runtime container to your tests in following ways.

First of all you can use a JUnit class rule and add the container to your test. 

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
                .withName("timer-to-log-export")
                .fromExport(TimerToLog_IT.class.getResourceAsStream("TimerToLog-export.zip"))
                .build();
```

This creates a new Syndesis runtime container and starts the integration from an export file `TimerToLog-export.zip`. This integration runtime container is shared for all test methods 
in that test class. 

In case you do require an explicit runtime container within a test method you can just initialize the container and start it.

```java
@Test
public void timeToLogExportTest() {
    SyndesisIntegrationRuntimeContainer.Builder integrationContainerBuilder = new SyndesisIntegrationRuntimeContainer.Builder()
            .withName("timer-to-log-export")
            .fromExport(TimerToLog_IT.class.getResourceAsStream("TimerToLog-export.zip"));

    try (SyndesisIntegrationRuntimeContainer integrationContainer = integrationContainerBuilder.build()) {
        integrationContainer.start();
    
        //do something with the integration runtime container
    }
}   
``` 

The `try-with-resources` block ensures that the container is stopped once the test is finished.

### From integration export

You can run exported integrations in the runtime container. This is the most convenient way to start the integration as every information required to run the integration is bundled in the
export file. You can customize the integration properties though using integration customizers.

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
                .withName("timer-to-log-export")
                .fromExport(TimerToLog_IT.class.getResourceAsStream("TimerToLog-export.zip"))
                .build();
```
 
### From integration model

You can create the integration model and run that integration in the runtime container. The integration model can be seen easily within the test and you can
create variations of that integration very easy.

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
    .withName("timer-to-log")
    .fromFlow(new Flow.Builder()
            .steps(Arrays.asList(new Step.Builder()
                    .stepKind(StepKind.endpoint)
                    .connection(new Connection.Builder()
                        .id("timer-connection")
                        .connector(new Connector.Builder()
                            .id("timer")
                            .putProperty("period",
                                new ConfigurationProperty.Builder()
                                        .kind("property")
                                        .secret(false)
                                        .componentProperty(false)
                                        .build())
                            .build())
                        .build())
                    .putConfiguredProperty("period", "1000")
                    .action(new ConnectorAction.Builder()
                        .id("periodic-timer-action")
                        .descriptor(new ConnectorDescriptor.Builder()
                            .connectorId("timer")
                            .componentScheme("timer")
                            .putConfiguredProperty("timer-name", "syndesis-timer")
                            .build())
                        .build())
                    .build(),
                new Step.Builder()
                    .stepKind(StepKind.log)
                    .putConfiguredProperty("bodyLoggingEnabled", "false")
                    .putConfiguredProperty("contextLoggingEnabled", "false")
                    .putConfiguredProperty("customText", "Hello Syndesis!")
                    .build()))
            .build())
        .build();
``` 

### From integration fat jar 

If you have a integration project fat jar you can build the integration runtime container directly with that jar file.

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
                .withName("timer-to-log-jar")
                .fromFatJar(Paths.get("/path/to/project.jar"))
                .build();
```

### From integration project 

You can build a runtime container from a Syndesis integration project folder. The project should contain all resources required to run the integration.
The integration runtime container will use a volume mount to that directory.

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
                .withName("timer-to-log-dir")
                .fromProjectDir(Paths.get("/path/to/project-dir"))
                .build();
```

### From integration json 

Last not least you can provide a Json model file representing the integration to build an run in the container.

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
                .withName("timer-to-log-json")
                .fromSupplier(new JsonIntegrationSupplier(TimerToLog_IT.class.getResource("TimerToLog.json")))
                .build();
```

## Syndesis db container

Syndesis uses a database to store integrations and connections in a Postgres storage. The integration tests provide a Postgres Testcontainer that is
configured with the proper `syndesis` database and user.

In addition to that the container defines some `sampledb` database holding two tables `todo` and `contact`. These sample tables are used by integrations when
testing data persistence with SQL connectors.

The integration tests automatically start the `syndesis-db` Testcontainer. All tests share the same container.

In case an integration runtime container needs access to the database you can add container networking as follows:

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
                        .withName("webhook-to-db")
                        .fromExport(WebHookToDB_IT.class.getResourceAsStream("WebhookToDB-export.zip"))
                        .build()
                        .withNetwork(getSyndesisDb().getNetwork())
                        .withExposedPorts(8080);
```

You can use `withNetwork(getSyndesisDb().getNetwork())` to access the database from a running integration. The `syndesis-db` container uses proper network aliases to ensure that
the integration is able to connect using the default SQL connector settings.

## Syndesis server

This container starts the Syndesis backend server. The container connects to the database container and provides REST services usually called via the Syndesis UI. The
container starts with some default properties set:

```
encrypt.key=supersecret
controllers.dblogging.enabled=false
openshift.enabled=false
metrics.kind=noop
features.monitoring.enabled=false
```

You can add/overwrite settings while creating the container:

```java
@ClassRule
public static SyndesisServerContainer syndesisServerContainer = new SyndesisServerContainer.Builder()
        .withJavaOption("encrypt.key", "something-different")
        .build()
        .withNetwork(getSyndesisDb().getNetwork());
```

By default the server container uses the Docker image `syndesis/syndesis-server:latest`. You can customize the image tag that should be used in order to start a different
release version of the Syndesis backend server. The integration test will pull the Docker image if not present on your host. See the list ov available 
[image tags for syndesis-server](https://hub.docker.com/r/syndesis/syndesis-server/tags).

When building a local server version you can also provide the path to a local `syndesis-server.jar`:

```java
@ClassRule
public static SyndesisServerContainer syndesisServerContainer = new SyndesisServerContainer.Builder()
        .withClasspathServerJar("path/to/server-runtime.jar")
        .build()
        .withNetwork(getSyndesisDb().getNetwork());
```

Instead of building the `server-runtime.jar` on your own you can also copy the jar from Maven central or your local Maven repository.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
      <execution>
        <id>copy</id>
        <phase>prepare-package</phase>
        <goals>
          <goal>copy</goal>
        </goals>
      </execution>
    </executions>
    <configuration>
      <skip>false</skip>
      <artifactItems>
        <artifactItem>
          <groupId>io.syndesis.server</groupId>
          <artifactId>server-runtime</artifactId>
          <version>${syndesis.version}</version>
          <type>jar</type>
          <outputDirectory>${project.build.testOutputDirectory}</outputDirectory>
          <destFileName>server-runtime.jar</destFileName>
        </artifactItem>
      </artifactItems>
    </configuration>
</plugin>        
```

This copies the `server-runtime.jar` with version `${syndesis.version}` to the test output directory. Now you can use the jar in your test using `withClasspathServerJar("server-runtime.jar")`.

## Infrastructure containers

### AMQ message broker

Some integrations connect to a AMQ message broker. The integration test project provides a JBoss AMQ container that is ready to be used with integration runtimes. In case
your integration requires the message broker you can add it to the test as JUnit class rule as follows:

```java
@ClassRule
public static JBossAMQBrokerContainer amqBrokerContainer = new JBossAMQBrokerContainer();
```

The AMQ broker container exports following ports and services:

```
withExposedPorts(61616);//openwire
withExposedPorts(61613);//stomp
withExposedPorts(5672);//amqp
withExposedPorts(1883);//mqtt
withExposedPorts(8778);//jolokia
```

In case your integration requires access to the message broker container you should add a networking to the container when building the integration runtime container. 
As usual this is done using `withNetwork(amqBrokerContainer.getNetwork())` configuration:

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
        .withName("amq-to-http")
        .fromExport(AMQToHttp_IT.class.getResourceAsStream("AMQToHttp-export.zip"))
        .customize("$..configuredProperties.baseUrl",
                    String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, todoServerPort))
        .build()
        .withNetwork(amqBrokerContainer.getNetwork());
```

## Simulate 3rd party interfaces

Many integrations connect to the outside world consuming services that are provided by 3rd party vendors (such as Twitter, Google, Salesforce, etc.) When testing those
integrations we need to simulate the 3rd party services as we do not want the integration tests to connect to the real 3rd party services. 

The integration tests use Citrus as base simulation framework for this task.

You can add Citrus components (Http services, AMQ consumers and so on) to your tests using Spring configurations:

```java
@Configuration
public static class EndpointConfig {
    @Bean
    public HttpServer todoApiServer() {
        return CitrusEndpoints.http()
                .server()
                .port(todoServerPort)
                .autoStart(true)
                .timeout(60000L)
                .build();
    }
}
```

The sample above creates a Citrus Http server listening on a dynamic tcp port. As we want to connect to that simulated service from within the integration runtime container we
need to expose this port to the Testcontainer runtime:

```java
private static int todoServerPort = SocketUtils.findAvailableTcpPort();
static {
    Testcontainers.exposeHostPorts(todoServerPort);
}
```

Testcontainers such as our integration runtime container can now connect to the port using the special host name `host.testcontainers.internal`:

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
        .withName("http-to-http")
        .fromExport(HttpToHttp_IT.class.getResourceAsStream("HttpToHttp-export.zip"))
        .customize("$..configuredProperties.baseUrl",
                    String.format("http:/host.testcontainers.internal:%s", todoServerPort))
        .build();
```

As you can see we customize the integration configured properties to use the simulated Citrus service endpoint as base URL. This way the integration connects to the
Citrus service instead of calling the real production endpoint.

The Citrus components usually provide services that are used by the integrations in order to control/verify the exchanged data. You tell the Citrus components what data to expect 
and return with the test runner Java DSL. As the base integration test is using Citrus functionality we can just inject the test runner to the test method.

```java
@Test
@CitrusTest
public void testHttpToHttp(@CitrusResource TestRunner runner) {
    runner.http(builder -> builder.server(todoApiServer)
            .receive()
            .get("/todos"));

    runner.http(builder -> builder.server(todoApiServer)
            .send()
            .response(HttpStatus.OK)
            .payload("[{\"id\": \"1\", \"task\":\"Learn to play drums\", \"completed\": 0}," +
                      "{\"id\": \"2\", \"task\":\"Learn to play guitar\", \"completed\": 0}," +
                      "{\"id\": \"3\", \"task\":\"Important: Learn to play piano\", \"completed\": 0}]"));
}
```

The test expects an incoming `GET` request on `/todos` on the simulated Citrus service. Citrus the is supposed to respond with a sample list of todo tasks.

This way wen can control the test data returned by 3rd party services and we implicitly validate that the integration connects to the 3rd party services.

## Customize integrations

### URLs and destinations

The exported integration may connect to 3rd party services. The services to connect to are defined with base URLs in the export. We have to overwrite
these URLs for the integration test because we want the integration to connect to a simulated 3rd party service instead of the production endpoint.

You can overwrite any configured property in the integration export using JsonPath expressions:

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
        .withName("http-to-google-sheets")
        .fromExport(HttpToHttp_IT.class.getResourceAsStream("HttpToGoogleSheets-export.zip"))
        .customize("$..configuredProperties.baseUrl",
                String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, todoServerPort))
        .customize("$..rootUrl.defaultValue",
                String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, googleSheetsServerPort))        
        .build();
```

While building the integration runtime container we can add JsonPath expressions that customize the exported integration. This enables us to set any configured
property in the integration export. In the sample above we overwrite the Http service base URL that is periodically called as start connection. In addition to that
we overwrite the Google Sheets root URL and point to the local simulated 3rd party services.

The Google Sheets connection also uses encrypted user credentials and secrets in the integration export. These credentials get automatically overwritten before the test.

### Encrypted secrets

The integration exports may use encrypted credentials representing passwords and secrets for connections to 3rd party services. The integration test 
automatically overwrites the encrypted values with a static secret ("secret"). This way simulated databases, infrastructure components (such as message brokers)
and 3rd party services can use the default "secret" credential in order to word with the integration export.

## Scheduler expressions

Sometimes integrations get periodically invoked with timer or scheduler. The exported integrations may use minutes or hours delay settings which is not
applicable to automated tests. You can overwrite the timer period with integration customizers:

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
        .withName("http-to-amq")
        .fromExport(HttpToAMQ_IT.class.getResourceAsStream("HttpToAMQ-export.zip"))
        .customize("$..configuredProperties.schedulerExpression", "1000")
        .customize("$..configuredProperties.baseUrl",
                    String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, todoServerPort))
        .build()
        .withNetwork(amqBrokerContainer.getNetwork());
```

The expression `customize("$..configuredProperties.schedulerExpression", "1000")` overwrites the scheduler to fire every 1000 milliseconds. This will be more
sufficient to the automated integration tests in terms of avoiding long running tests.

## Logging

By default the integration tests log output to a file `target/integration-test.log`. You can also enable logging to the console in `src/main/resources/logback-test.xml`.

When running containers the log output is not visible by default. You need to enable logging on the container:

```java
@ClassRule
public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
        .withName("http-to-http")
        .fromExport(HttpToHttp_IT.class.getResourceAsStream("HttpToHttp-export.zip"))
        .enableLogging()
        .build();
``` 

The `enableLogging` setting enables container logging to the logback logger. By default the container logs are sent to a separate log file `target/integration-runtime.log`.