package io.syndesis.qe.itest.containers.integration;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.syndesis.common.model.integration.Flow;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.qe.itest.integration.customizer.IntegrationCustomizer;
import io.syndesis.qe.itest.containers.integration.project.ApplicationPropertiesProvider;
import io.syndesis.qe.itest.containers.integration.project.IntegrationProjectProvider;
import io.syndesis.qe.itest.containers.integration.project.S2IProjectProvider;
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

    private SyndesisIntegrationRuntimeContainer(String syndesisVersion, String integrationName, Path projectJar, String applicationProperties, boolean deleteOnExit) {
        super(new ImageFromDockerfile(integrationName, deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from(String.format("syndesis/syndesis-s2i:%s", syndesisVersion))
                        .add("integration-runtime.jar", "/deployments/integration-runtime.jar")
                        .add("application.properties", "/deployments/config/application.properties")
                        .build())
                        .withFileFromPath("integration-runtime.jar", projectJar.toAbsolutePath())
                        .withFileFromString("application.properties", applicationProperties));

        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(integrationName));
    }

    public static class Builder {
        private String name = "i-test-integration";
        private String syndesisVersion = "latest";

        private boolean deleteOnExit = true;

        private ApplicationPropertiesProvider propertiesProvider;
        private IntegrationProjectProvider projectProvider;
        private IntegrationSupplier integrationSupplier;

        private List<IntegrationCustomizer> customizers = new ArrayList<>();

        public SyndesisIntegrationRuntimeContainer build() {
            if (projectProvider == null) {
                projectProvider = new S2IProjectProvider(name, syndesisVersion);
            }

            CustomizerAwareIntegrationSupplier supplier = new CustomizerAwareIntegrationSupplier(integrationSupplier, customizers);
            Path projectJarPath = projectProvider.buildProject(supplier);

            if (propertiesProvider == null) {
                propertiesProvider = new S2IProjectProvider(name, syndesisVersion);
            }

            String applicationProperties = propertiesProvider.getApplicationProperties(supplier);

            return new SyndesisIntegrationRuntimeContainer(syndesisVersion, name, projectJarPath, applicationProperties, deleteOnExit);
        }

        public Builder withName(String name) {
            this.name = name.startsWith("i-") ? name : "i-" + name;
            return this;
        }

        public Builder withSyndesisVersion(String version) {
            this.syndesisVersion = version;
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
    }
}
