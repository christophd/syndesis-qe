package io.syndesis.qe.itest.container;

import io.syndesis.qe.itest.SyndesisIntegrationTestSupport;
import io.syndesis.qe.itest.containers.server.SyndesisServerContainer;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class SyndesisServerContainer_Tester extends SyndesisIntegrationTestSupport {

    @Test
    public void testSyndesisServer() {
        try (SyndesisServerContainer serverContainer = new SyndesisServerContainer.Builder()
                .withClasspathServerJar("server-runtime.jar")
                .enableLogging()
                .build()
                .withNetwork(getSyndesisDb().getNetwork())) {

            serverContainer.start();
        }
    }
}
