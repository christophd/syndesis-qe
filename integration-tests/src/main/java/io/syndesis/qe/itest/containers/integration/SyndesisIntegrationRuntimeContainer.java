package io.syndesis.qe.itest.containers.integration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.syndesis.common.model.integration.Flow;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.qe.itest.SyndesisTestEnvironment;
import io.syndesis.qe.itest.containers.s2i.S2iProjectBuilder;
import io.syndesis.qe.itest.integration.customizer.IntegrationCustomizer;
import io.syndesis.qe.itest.integration.customizer.JsonPathIntegrationCustomizer;
import io.syndesis.qe.itest.integration.project.ProjectBuilder;
import io.syndesis.qe.itest.integration.project.SpringBootProjectBuilder;
import io.syndesis.qe.itest.integration.source.CustomizedIntegrationSource;
import io.syndesis.qe.itest.integration.source.IntegrationExportSource;
import io.syndesis.qe.itest.integration.source.IntegrationSource;
import io.syndesis.qe.itest.integration.source.JsonIntegrationSource;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Container that executes a integration runtime. The container is either provided with a runnable project fat jar or a project directory
 * holding the sources to run the integration.
 *
 * All Syndesis dependencies (artifacts, 3rd party libs) are already bundled with the syndesis-s2i base image.
 *
 * When using a fat jar the fabric8 S2i run script is called to executes the jar. When using a project source directory a plain Spring Boot
 * run command is used to build and run the sources.
 *
 * @author Christoph Deppisch
 */
public class SyndesisIntegrationRuntimeContainer extends GenericContainer<SyndesisIntegrationRuntimeContainer> {

    public static final int MANAGEMENT_PORT = 8081;
    public static final int SERVER_PORT = 8080;
    private static final String S2I_RUN_SCRIPT = "/usr/local/s2i/run";

    private String internalHostIp;

    /**
     * Uses Spring Boot Maven build to run the integration project. Much faster as S2i build because we can directly use the project sources.
     *
     * @param imageTag
     * @param integrationName
     * @param projectDir
     * @param deleteOnExit
     * @param debugEnabled
     */
    private SyndesisIntegrationRuntimeContainer(String imageTag, String integrationName, Path projectDir, boolean deleteOnExit, boolean debugEnabled) {
        super(new ImageFromDockerfile(integrationName, deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", imageTag))
                        .expose(SyndesisTestEnvironment.getDebugPort())
                        .cmd(getMavenCommandLine(debugEnabled))
                        .build()));

        withFileSystemBind(projectDir.toAbsolutePath().toString(), "/tmp/src", BindMode.READ_WRITE);
        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(integrationName));
    }

    /**
     * Uses project fat jar to run integration. Runs the Java application with run script provided by the Syndesis S2i image.
     * @param imageTag
     * @param integrationName
     * @param projectJar
     * @param deleteOnExit
     * @param debugEnabled
     */
    private SyndesisIntegrationRuntimeContainer(String imageTag, String integrationName, File projectJar, boolean deleteOnExit, boolean debugEnabled) {
        super(new ImageFromDockerfile(integrationName, deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", imageTag))
                        .add("integration-runtime.jar", "/deployments/integration-runtime.jar")
                        .env("JAVA_OPTIONS", getDebugAgentOption(debugEnabled))
                        .expose(SyndesisTestEnvironment.getDebugPort())
                        .cmd(getS2iRunCommandLine(debugEnabled))
                        .build())
                        .withFileFromPath("integration-runtime.jar", projectJar.toPath().toAbsolutePath()));

        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(integrationName));
    }

    private static String getMavenCommandLine(boolean debugEnabled) {
        StringJoiner commandLine = new StringJoiner(" ");

        commandLine.add("mvn")
                   .add("-s")
                   .add("/tmp/src/configuration/settings.xml")
                   .add("-f")
                   .add("/tmp/src")
                   .add("spring-boot:run")
                   .add("-Dmaven.repo.local=/tmp/artifacts/m2");

        if (debugEnabled) {
            commandLine.add(getDebugJvmArguments(debugEnabled));
        }

        return commandLine.toString();
    }

    private static String getDebugJvmArguments(boolean debugEnabled) {
        return debugEnabled ? String.format("-Drun.jvmArguments=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s\"", SyndesisTestEnvironment.getDebugPort()) : "";
    }

    private static String getS2iRunCommandLine(boolean debugEnabled) {
        StringJoiner commandLine = new StringJoiner(" ");

        commandLine.add(S2I_RUN_SCRIPT);

        if (debugEnabled) {
            commandLine.add("--debug");
        }

        return commandLine.toString();
    }

    private static String getDebugAgentOption(boolean debugEnabled) {
        return debugEnabled ? String.format("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=%s", SyndesisTestEnvironment.getDebugPort()) : "";
    }

    public static class Builder {
        private String name = "i-test-integration";
        private String syndesisVersion = SyndesisTestEnvironment.getSyndesisVersion();
        private String imageTag = SyndesisTestEnvironment.getSyndesisImageTag();

        private boolean deleteOnExit = true;
        private boolean enableLogging = SyndesisTestEnvironment.isLoggingEnabled();
        private boolean enableDebug = SyndesisTestEnvironment.isDebugEnabled();

        private ProjectBuilder projectBuilder;
        private IntegrationSource integrationSource;

        private List<IntegrationCustomizer> customizers = new ArrayList<>();

        public SyndesisIntegrationRuntimeContainer build() {
            CustomizedIntegrationSource source = new CustomizedIntegrationSource(integrationSource, customizers);
            Path projectPath = getProjectBuilder().build(source);

            SyndesisIntegrationRuntimeContainer container;
            if (Files.isDirectory(projectPath)) {
                //Run directly from project source directory
                container = new SyndesisIntegrationRuntimeContainer(imageTag, name, projectPath, deleteOnExit, enableDebug);
            } else {
                //Run project fat jar
                container = new SyndesisIntegrationRuntimeContainer(imageTag, name, projectPath.toFile(), deleteOnExit, enableDebug);
            }

            if (enableLogging) {
                container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("INTEGRATION_RUNTIME_CONTAINER")));
            }

            if (enableDebug) {
                container.withExposedPorts(SyndesisTestEnvironment.getDebugPort());
                container.withCreateContainerCmdModifier(cmd -> cmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(SyndesisTestEnvironment.getDebugPort()), new ExposedPort(SyndesisTestEnvironment.getDebugPort()))));
            }

            return container;
        }

        public Builder name(String name) {
            this.name = name.startsWith("i-") ? name : "i-" + name;
            return this;
        }

        public Builder syndesisVersion(String version) {
            this.syndesisVersion = version;
            return this;
        }

        public Builder imageTag(String tag) {
            this.imageTag = tag;
            return this;
        }

        public Builder deleteOnExit(boolean deleteOnExit) {
            this.deleteOnExit = deleteOnExit;
            return this;
        }

        public Builder withProjectBuilder(ProjectBuilder builder) {
            this.projectBuilder = builder;
            return this;
        }

        public Builder fromIntegration(Integration integration) {
            this.integrationSource = () -> integration;
            return this;
        }

        public Builder fromSource(IntegrationSource source) {
            integrationSource = source;
            return this;
        }

        public Builder fromFlows(Flow ... integrationFlows) {
            this.integrationSource = () -> new Integration.Builder()
                    .id(this.name)
                    .name("Test Integration")
                    .description("This is a test integration!")
                    .addFlows(integrationFlows)
                    .build();

            return this;
        }

        public Builder fromFlow(Flow integrationFlow) {
            return fromFlows(integrationFlow);
        }

        public Builder fromJson(String json) {
            integrationSource = new JsonIntegrationSource(json);
            return this;
        }

        public Builder fromJson(Path pathToJson) {
            integrationSource = new JsonIntegrationSource(pathToJson);
            return this;
        }

        public Builder fromFatJar(Path pathToJar) {
            this.integrationSource = () -> null;
            this.projectBuilder = (integration) -> pathToJar;
            return this;
        }

        public Builder fromProjectDir(Path pathToProject) {
            this.integrationSource = () -> null;
            this.projectBuilder = (integration) -> pathToProject;
            return this;
        }

        public Builder fromExport(Path pathToExport) {
            this.integrationSource = new IntegrationExportSource(pathToExport);
            return this;
        }

        public Builder fromExport(InputStream export) {
            this.integrationSource = new IntegrationExportSource(export);
            return this;
        }

        public Builder customize(String expression, Object value) {
            return customize(new JsonPathIntegrationCustomizer(expression, value));
        }

        public Builder customize(String expression, String key, Object value) {
            return customize(new JsonPathIntegrationCustomizer(expression, key, value));
        }

        public Builder customize(IntegrationCustomizer customizer) {
            this.customizers.add(customizer);
            return this;
        }

        public Builder enableLogging() {
            this.enableLogging = true;
            return this;
        }

        public Builder enableDebug() {
            this.enableDebug = true;
            return this;
        }

        private ProjectBuilder getProjectBuilder() {
            if (projectBuilder != null) {
                return projectBuilder;
            }

            ProjectBuilder builder = new SpringBootProjectBuilder(name, syndesisVersion);
            if (SyndesisTestEnvironment.isS2iBuildEnabled()) {
                return new S2iProjectBuilder(builder, imageTag);
            } else {
                return builder;
            }
        }
    }

    public int getServerPort() {
        return getMappedPort(SERVER_PORT);
    }

    public int getManagementPort() {
        return getMappedPort(MANAGEMENT_PORT);
    }

    public String getInternalHostIp() {
        return internalHostIp;
    }

    @Override
    public SyndesisIntegrationRuntimeContainer withExtraHost(String hostname, String ipAddress) {
        if (INTERNAL_HOST_HOSTNAME.equals(hostname)) {
            this.internalHostIp = ipAddress;
        }

        return super.withExtraHost(hostname, ipAddress);
    }
}
