package io.syndesis.qe.itest.containers.server;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Christoph Deppisch
 */
public class SyndesisServerContainer extends GenericContainer<SyndesisServerContainer> {

    public SyndesisServerContainer() {
        super(new ImageFromDockerfile("syndesis-server", false)
                .withDockerfileFromBuilder(builder -> builder.from("fabric8/s2i-java:3.0-java8")
                                                             .env("JAVA_APP_JAR", "server-runtime.jar")
                                                             .env("JAVA_OPTIONS", "-Dencrypt.key=supersecret -Dcontrollers.dblogging.enabled=false -Dopenshift.enabled=false -Dmetrics.kind=noop -Dfeatures.monitoring.enabled=false")
                                                             .build()));

        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("syndesis-server"));

        withClasspathResourceMapping("server-runtime.jar","/deployments/server-runtime.jar", BindMode.READ_ONLY);
        withExposedPorts(8080);
    }

}
