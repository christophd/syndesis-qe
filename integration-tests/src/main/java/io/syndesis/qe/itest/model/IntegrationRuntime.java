package io.syndesis.qe.itest.model;

import java.util.stream.Stream;

import io.syndesis.qe.itest.integration.project.CamelKProjectBuilder;
import io.syndesis.qe.itest.integration.project.ProjectBuilder;
import io.syndesis.qe.itest.integration.project.SpringBootProjectBuilder;
import io.syndesis.server.endpoint.v1.VersionService;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

/**
 * Available integration runtimes.
 */
public enum IntegrationRuntime {
    SPRING_BOOT("spring-boot",
            "spring-boot:run",
            Wait.forLogMessage(".*Started Application.*\\s", 1),
            SpringBootProjectBuilder::new),
    CAMEL_K("camel-k",
            "process-resources exec:java",
            Wait.forLogMessage(".*Apache Camel " + new VersionService().getCamelVersion() + ".* started.*\\s", 1),
            CamelKProjectBuilder::new);

    private final String id;
    private final String command;
    private final WaitStrategy readinessProbe;
    private final ProjectBuilderSupplier projectBuilderSupplier;

    IntegrationRuntime(String id, String command, WaitStrategy readinessProbe, ProjectBuilderSupplier projectBuilderSupplier) {
        this.id = id;
        this.command = command;
        this.readinessProbe = readinessProbe;
        this.projectBuilderSupplier = projectBuilderSupplier;
    }

    public String getCommand() {
        return command;
    }

    public String getId() {
        return id;
    }

    public static IntegrationRuntime fromId(String id) {
        return Stream.of(IntegrationRuntime.values())
                .filter(runtime -> runtime.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported integration runtime '%s'", id)));
    }

    public WaitStrategy getReadinessProbe() {
        return readinessProbe;
    }

    public ProjectBuilder getProjectBuilder(String name, String syndesisVersion) {
        return projectBuilderSupplier.get(name, syndesisVersion);
    }

    interface ProjectBuilderSupplier {
        ProjectBuilder get(String name, String syndesisVersion);
    }
}
