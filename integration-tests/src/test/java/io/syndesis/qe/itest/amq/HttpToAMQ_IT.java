package io.syndesis.qe.itest.amq;

import javax.jms.ConnectionFactory;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import io.syndesis.qe.itest.SyndesisIntegrationTestSupport;
import io.syndesis.qe.itest.containers.amq.JBossAMQBrokerContainer;
import io.syndesis.qe.itest.containers.integration.SyndesisIntegrationRuntimeContainer;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.SocketUtils;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration(classes = HttpToAMQ_IT.EndpointConfig.class)
public class HttpToAMQ_IT extends SyndesisIntegrationTestSupport {

    private static int todoServerPort = SocketUtils.findAvailableTcpPort();
    static {
        Testcontainers.exposeHostPorts(todoServerPort);
    }

    @Autowired
    private HttpServer todoApiServer;

    @Autowired
    private JmsEndpoint todoJms;

    @ClassRule
    public static JBossAMQBrokerContainer amqBrokerContainer = new JBossAMQBrokerContainer();

    /**
     * Integration periodically requests list of tasks (as Json array) from Http service and maps the results to AMQ queue.
     * AMQ queue is provided with all tasks (Json array) as message payload.
     */
    @ClassRule
    public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
            .withName("http-to-amq")
            .fromExport(HttpToAMQ_IT.class.getResourceAsStream("HttpToAMQ-export.zip"))
            .customize("$..configuredProperties.baseUrl",
                        String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, todoServerPort))
            .build()
            .withNetwork(amqBrokerContainer.getNetwork());

    @Test
    @CitrusTest
    public void testHttpToAMQ(@CitrusResource TestRunner runner) {
        runner.http(builder -> builder.server(todoApiServer)
                .receive()
                .get());

        runner.http(builder -> builder.server(todoApiServer)
                .send()
                .response(HttpStatus.OK)
                .payload("[{\"id\": \"1\", \"task\":\"Learn to play drums\", \"completed\": 0}," +
                          "{\"id\": \"2\", \"task\":\"Learn to play guitar\", \"completed\": 1}]"));

        runner.receive(builder -> builder.endpoint(todoJms)
                        .payload("[{\"id\": \"1\", \"name\":\"Learn to play drums\", \"done\": 0}," +
                                  "{\"id\": \"2\", \"name\":\"Learn to play guitar\", \"done\": 1}]"));
    }

    @Configuration
    public static class EndpointConfig {
        @Bean
        public ConnectionFactory connectionFactory() {
            return new ActiveMQConnectionFactory(amqBrokerContainer.getUsername(),
                                                 amqBrokerContainer.getPassword(),
                                                 String.format("tcp://localhost:%s", amqBrokerContainer.getOpenwirePort()));
        }

        @Bean
        public JmsEndpoint todoJms() {
            return CitrusEndpoints.jms()
                    .asynchronous()
                    .connectionFactory(connectionFactory())
                    .destination("todos")
                    .build();
        }

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
}