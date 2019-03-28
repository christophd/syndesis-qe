package io.syndesis.qe.itest.containers.amq;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * @author Christoph Deppisch
 */
public class JBossAMQBrokerContainer extends GenericContainer<JBossAMQBrokerContainer> {

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

        withExposedPorts(61616);//openwire
        withExposedPorts(61613);//stomp
        withExposedPorts(5672);//amqp
        withExposedPorts(1883);//mqtt
        withExposedPorts(8778);//jolokia

        withNetwork(Network.newNetwork());
        withNetworkAliases("broker-amq-tcp");

        withCreateContainerCmdModifier(cmd -> cmd.withName("broker-amq"));

        waitingFor(Wait.forLogMessage(".*Apache ActiveMQ.*started.*\\s", 1));
    }

    public int getOpenwirePort() {
        return getMappedPort(61616);
    }

    public int getStompPort() {
        return getMappedPort(61613);
    }

    public int getAmqpPort() {
        return getMappedPort(5672);
    }

    public int getMqttPort() {
        return getMappedPort(1883);
    }

    public int getJolokiaPort() {
        return getMappedPort(8778);
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
