package io.syndesis.qe.itest.containers.amq;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * @author Christoph Deppisch
 */
public class JBossAMQBrokerContainer extends GenericContainer<JBossAMQBrokerContainer> {

    private static final int OPENWIRE_PORT = 61616;
    private static final int STOMP_PORT = 61613;
    private static final int AMQP_PORT = 5672;
    private static final int MQTT_PORT = 1883;
    private static final int JOLOKIA_PORT = 8778;

    private final String username = "amq";
    private final String password = "secret";

    public JBossAMQBrokerContainer() {
        this("1.3");
    }

    public JBossAMQBrokerContainer(String imageVersion) {
        super(String.format("registry.access.redhat.com/jboss-amq-6/amq63-openshift:%s", imageVersion));

        withEnv("AMQ_USER", username);
        withEnv("AMQ_PASSWORD", password);
        withEnv("AMQ_TRANSPORTS", "openwire,stomp,amqp,mqtt");

        withExposedPorts(OPENWIRE_PORT);
        withExposedPorts(STOMP_PORT);
        withExposedPorts(AMQP_PORT);
        withExposedPorts(MQTT_PORT);
        withExposedPorts(JOLOKIA_PORT);

        withNetwork(Network.newNetwork());
        withNetworkAliases("broker-amq-tcp");

        withCreateContainerCmdModifier(cmd -> cmd.withName("broker-amq"));

        waitingFor(Wait.forLogMessage(".*Apache ActiveMQ.*started.*\\s", 1));
    }

    public int getOpenwirePort() {
        return getMappedPort(OPENWIRE_PORT);
    }

    public int getStompPort() {
        return getMappedPort(STOMP_PORT);
    }

    public int getAmqpPort() {
        return getMappedPort(AMQP_PORT);
    }

    public int getMqttPort() {
        return getMappedPort(MQTT_PORT);
    }

    public int getJolokiaPort() {
        return getMappedPort(JOLOKIA_PORT);
    }

    /**
     * Obtains the username.
     *
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Obtains the password.
     *
     * @return
     */
    public String getPassword() {
        return password;
    }
}
