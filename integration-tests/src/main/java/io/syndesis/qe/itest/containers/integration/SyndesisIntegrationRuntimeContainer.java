package io.syndesis.qe.itest.containers.integration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.syndesis.common.model.integration.Flow;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.qe.itest.SyndesisTestEnvironment;
import io.syndesis.qe.itest.containers.integration.project.IntegrationProjectProvider;
import io.syndesis.qe.itest.containers.integration.project.ProjectProvider;
import io.syndesis.qe.itest.containers.integration.project.S2iProjectProvider;
import io.syndesis.qe.itest.integration.customizer.IntegrationCustomizer;
import io.syndesis.qe.itest.integration.supplier.CustomizerAwareIntegrationSupplier;
import io.syndesis.qe.itest.integration.supplier.ExportIntegrationSupplier;
import io.syndesis.qe.itest.integration.supplier.IntegrationSupplier;
import io.syndesis.qe.itest.integration.supplier.JsonIntegrationSupplier;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Christoph Deppisch
 */
public class SyndesisIntegrationRuntimeContainer extends GenericContainer<SyndesisIntegrationRuntimeContainer> {

    /**
     * Uses Spring Boot Maven build to run the integration project. Much faster as S2i build because we can directly use the project sources.
     *
     * @param imageTag
     * @param integrationName
     * @param projectDir
     * @param deleteOnExit
     */
    private SyndesisIntegrationRuntimeContainer(String imageTag, String integrationName, Path projectDir, boolean deleteOnExit) {
        super(new ImageFromDockerfile(integrationName, deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", imageTag))
                        .cmd("mvn", "-s", "/tmp/src/configuration/settings.xml", "-f", "/tmp/src", "spring-boot:run", "-Dmaven.repo.local=/tmp/artifacts/m2")
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
     */
    private SyndesisIntegrationRuntimeContainer(String imageTag, String integrationName, File projectJar, boolean deleteOnExit) {
        super(new ImageFromDockerfile(integrationName, deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", imageTag))
                        .add("integration-runtime.jar", "/deployments/integration-runtime.jar")
                        .build())
                        .withFileFromPath("integration-runtime.jar", projectJar.toPath().toAbsolutePath()));

        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(integrationName));
    }

    public static class Builder {
        private String name = "i-test-integration";
        private String syndesisVersion = SyndesisTestEnvironment.getSyndesisVersion();
        private String imageTag = SyndesisTestEnvironment.getSyndesisImageTag();

        private boolean deleteOnExit = true;

        private IntegrationProjectProvider projectProvider;
        private IntegrationSupplier integrationSupplier;

        private List<IntegrationCustomizer> customizers = new ArrayList<>();

        public SyndesisIntegrationRuntimeContainer build() {
            CustomizerAwareIntegrationSupplier supplier = new CustomizerAwareIntegrationSupplier(integrationSupplier, customizers);
            Path projectPath = getProjectProvider().buildProject(supplier);

            if (Files.isDirectory(projectPath)) {
                //Run directly from project source directory
                return new SyndesisIntegrationRuntimeContainer(imageTag, name, projectPath, deleteOnExit);
            } else {
                //Run project fat jar
                return new SyndesisIntegrationRuntimeContainer(imageTag, name, projectPath.toFile(), deleteOnExit);
            }
        }

        public Builder withName(String name) {
            this.name = name.startsWith("i-") ? name : "i-" + name;
            return this;
        }

        public Builder withSyndesisVersion(String version) {
            this.syndesisVersion = version;
            return this;
        }

        public Builder withImageTag(String tag) {
            this.imageTag = tag;
            return this;
        }

        public Builder withDeleteOnExit(boolean deleteOnExit) {
            this.deleteOnExit = deleteOnExit;
            return this;
        }

        public Builder withProjectProvider(IntegrationProjectProvider provider) {
            this.projectProvider = provider;
            return this;
        }

        public Builder fromIntegration(Integration integration) {
            this.integrationSupplier = () -> integration;
            return this;
        }

        public Builder fromSupplier(IntegrationSupplier supplier) {
            integrationSupplier = supplier;
            return this;
        }

        public Builder fromFlows(Flow ... integrationFlows) {
            this.integrationSupplier = () -> new Integration.Builder()
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
            integrationSupplier = new JsonIntegrationSupplier(json);
            return this;
        }

        public Builder fromJson(Path pathToJson) {
            integrationSupplier = new JsonIntegrationSupplier(pathToJson);
            return this;
        }

        public Builder fromFatJar(Path pathToJar) {
            this.integrationSupplier = () -> null;
            this.projectProvider = (integration) -> pathToJar;
            return this;
        }

        public Builder fromExport(Path pathToExport) {
            this.integrationSupplier = new ExportIntegrationSupplier(pathToExport);
            return this;
        }

        public Builder fromExport(InputStream export) {
            this.integrationSupplier = new ExportIntegrationSupplier(export);
            return this;
        }

        public Builder withIntegrationCustomizer(IntegrationCustomizer customizer) {
            this.customizers.add(customizer);
            return this;
        }

        private IntegrationProjectProvider getProjectProvider() {
            if (projectProvider != null) {
                return projectProvider;
            }

            if (SyndesisTestEnvironment.isS2iBuildEnabled()) {
                return new S2iProjectProvider(name, syndesisVersion, imageTag);
            } else {
                return new ProjectProvider(name, syndesisVersion);
            }
        }
    }
}
