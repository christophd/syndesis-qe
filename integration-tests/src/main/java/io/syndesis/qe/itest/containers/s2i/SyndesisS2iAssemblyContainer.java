package io.syndesis.qe.itest.containers.s2i;

import java.nio.file.Path;
import java.time.Duration;

import io.syndesis.qe.itest.SyndesisTestEnvironment;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Syndesis S2i container that performs assemble step on a give project directory. The project sources are assembled to
 * a runnable project fat jar using fabric8 S2i assemble script.
 *
 * The container uses the Syndesis S2i image as base. This image already holds all required Syndesis libraries and artifacts
 * with the given version.
 *
 * @author Christoph Deppisch
 */
public class SyndesisS2iAssemblyContainer extends GenericContainer<SyndesisS2iAssemblyContainer> {

    private static final String S2I_ASSEMBLE_SCRIPT = "/usr/local/s2i/assemble";

    public SyndesisS2iAssemblyContainer(String integrationName, Path projectDir, String imageTag) {
        super(new ImageFromDockerfile(integrationName + "-s2i", true)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", imageTag))
                        .cmd(S2I_ASSEMBLE_SCRIPT)
                        .build()));

        withFileSystemBind(projectDir.toAbsolutePath().toString(), "/tmp/src", BindMode.READ_WRITE);
        waitingFor(new LogMessageWaitStrategy().withRegEx(".*\\.\\.\\. done.*\\s")
                                               .withStartupTimeout(Duration.ofSeconds(SyndesisTestEnvironment.getContainerStartupTimeout())));
    }
}
