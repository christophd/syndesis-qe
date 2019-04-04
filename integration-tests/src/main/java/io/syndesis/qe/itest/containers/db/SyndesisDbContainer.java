package io.syndesis.qe.itest.containers.db;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author Christoph Deppisch
 */
public class SyndesisDbContainer extends PostgreSQLContainer<SyndesisDbContainer> {

    public static final int DB_PORT = 5432;

    public SyndesisDbContainer() {
        withDatabaseName("sampledb");
        withUsername("sampledb");
        withPassword("secret");

        withCreateContainerCmdModifier(cmd -> cmd.withName("syndesis-db"));
        withCreateContainerCmdModifier(cmd -> cmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(DB_PORT), new ExposedPort(DB_PORT))));
        withInitScript("syndesis-db-init.sql");

        withNetwork(Network.newNetwork());
        withNetworkAliases("syndesis-db");
    }
}
