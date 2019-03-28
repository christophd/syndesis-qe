package io.syndesis.qe.itest.containers.s2i;

import java.nio.file.Path;
import java.time.Duration;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Christoph Deppisch
 */
public class SyndesisS2iContainer extends GenericContainer<SyndesisS2iContainer> {

    public SyndesisS2iContainer(String integrationName, Path projectDir, String syndesisVersion) {
        super(new ImageFromDockerfile(integrationName + "-s2i", true)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", syndesisVersion))
                        .cmd("/usr/local/s2i/assemble")
                        .build()));

        withFileSystemBind(projectDir.toAbsolutePath().toString(), "/tmp/src", BindMode.READ_WRITE);
        waitingFor(new LogMessageWaitStrategy().withRegEx(".*\\.\\.\\. done.*\\s")
                                               .withStartupTimeout(Duration.ofSeconds(120)));
    }
}
