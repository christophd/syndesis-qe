package io.syndesis.qe.itest.containers.integration;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.syndesis.common.model.integration.Flow;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.qe.itest.integration.customizer.IntegrationCustomizer;
import io.syndesis.qe.itest.containers.integration.project.ApplicationPropertiesProvider;
import io.syndesis.qe.itest.containers.integration.project.ProjectJarGenerator;
import io.syndesis.qe.itest.containers.integration.project.S2IProjectGenerator;
import io.syndesis.qe.itest.integration.supplier.CustomizerAwareIntegrationSupplier;
import io.syndesis.qe.itest.integration.supplier.ExportIntegrationSupplier;
import io.syndesis.qe.itest.integration.supplier.IntegrationSupplier;
import io.syndesis.qe.itest.integration.supplier.JsonIntegrationSupplier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Christoph Deppisch
 */
public class SyndesisIntegrationRuntimeContainer extends GenericContainer<SyndesisIntegrationRuntimeContainer> {

    private SyndesisIntegrationRuntimeContainer(String integrationName, Path projectJar, String applicationProperties, String s2iVersion, boolean deleteOnExit) {
        super(new ImageFromDockerfile(integrationName, deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", s2iVersion))
                        .add("integration-runtime.jar", "/deployments/integration-runtime.jar")
                        .add("application.properties", "/deployments/config/application.properties")
                        .build())
                        .withFileFromPath("integration-runtime.jar", projectJar.toAbsolutePath())
                        .withFileFromString("application.properties", applicationProperties));

        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(integrationName));
    }

    public static class Builder {
        private String name = "i-test-integration";
        private String s2iVersion = "latest";

        private boolean deleteOnExit = true;

        private ApplicationPropertiesProvider propertiesProvider;
        private ProjectJarGenerator projectJarGenerator;
        private IntegrationSupplier integrationSupplier;

        private List<IntegrationCustomizer> customizers = new ArrayList<>();

        public SyndesisIntegrationRuntimeContainer build() {
            if (projectJarGenerator == null) {
                projectJarGenerator = new S2IProjectGenerator(name, s2iVersion);
            }

            CustomizerAwareIntegrationSupplier supplier = new CustomizerAwareIntegrationSupplier(integrationSupplier, customizers);
            Path projectJarPath = projectJarGenerator.buildProjectJar(supplier);

            if (propertiesProvider == null) {
                propertiesProvider = new S2IProjectGenerator(name, s2iVersion);
            }

            String applicationProperties = propertiesProvider.getApplicationProperties(supplier);

            return new SyndesisIntegrationRuntimeContainer(name, projectJarPath, applicationProperties, s2iVersion, deleteOnExit);
        }

        public Builder withName(String name) {
            this.name = name.startsWith("i-") ? name : "i-" + name;
            return this;
        }

        public Builder withS2iVersion(String version) {
            this.s2iVersion = version;
            return this;
        }

        public Builder withDeleteOnExit(boolean deleteOnExit) {
            this.deleteOnExit = deleteOnExit;
            return this;
        }

        public Builder withJarGenerator(ProjectJarGenerator generator) {
            this.projectJarGenerator = generator;
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
            this.projectJarGenerator = (integration) -> pathToJar;
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
    }
}
