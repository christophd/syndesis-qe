package io.syndesis.qe.itest.containers.integration.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.extension.Extension;
import io.syndesis.common.model.openapi.OpenApi;
import io.syndesis.common.util.MavenProperties;
import io.syndesis.integration.api.IntegrationResourceManager;
import io.syndesis.integration.project.generator.ProjectGenerator;
import io.syndesis.integration.project.generator.ProjectGeneratorConfiguration;
import io.syndesis.qe.itest.integration.supplier.IntegrationSupplier;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * @author Christoph Deppisch
 */
public class ProjectProvider implements IntegrationProjectProvider, ApplicationPropertiesProvider {

    private final String name;

    private Path tmpDir;
    private ProjectGeneratorConfiguration projectGeneratorConfiguration = new ProjectGeneratorConfiguration();
    private MavenProperties mavenProperties = new MavenProperties();

    public ProjectProvider(String name) {
        this.name = name;
        withTempDirectory("target/integrations");
    }

    @Override
    public Path buildProject(IntegrationSupplier integrationSupplier) {
        try {
            Path projectDir = Files.createTempDirectory(tmpDir, name);
            ProjectGenerator projectGenerator = new ProjectGenerator(projectGeneratorConfiguration, new ProjectProvider.StaticIntegrationResourceManager(integrationSupplier), mavenProperties);
            try (TarArchiveInputStream in = new TarArchiveInputStream(projectGenerator.generate(integrationSupplier.get(), System.out::println))) {
                ArchiveEntry archiveEntry;
                while ((archiveEntry = in.getNextEntry()) != null) {
                    Path fileOrDirectory = projectDir.resolve(archiveEntry.getName());
                    if (archiveEntry.isDirectory()) {
                        if (!Files.exists(fileOrDirectory)) {
                            Files.createDirectories(fileOrDirectory);
                        }
                    } else {
                        Files.createDirectories(fileOrDirectory.getParent());
                        Files.copy(in, fileOrDirectory);
                    }
                }
            }

            return projectDir;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create integration project", e);
        }
    }

    @Override
    public String getApplicationProperties(IntegrationSupplier integrationSupplier) {
        try {
            ProjectGenerator projectGenerator = new ProjectGenerator(projectGeneratorConfiguration, new ProjectProvider.StaticIntegrationResourceManager(integrationSupplier), mavenProperties);
            Properties applicationProperties = projectGenerator.generateApplicationProperties(integrationSupplier.get());

            StringWriter writer = new StringWriter();
            applicationProperties.store(writer, "#Integration Test secrets");

            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create application properties", e);
        }
    }

    public ProjectProvider withTempDirectory(String tmpDir) {
        try {
            this.tmpDir = Files.createDirectories(Paths.get(tmpDir));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temp directory", e);
        }
        return this;
    }

    private static class StaticIntegrationResourceManager implements IntegrationResourceManager {
        private final IntegrationSupplier integrationSupplier;

        private StaticIntegrationResourceManager(IntegrationSupplier integrationSupplier) {
            this.integrationSupplier = integrationSupplier;
        }

        @Override
        public Optional<Connector> loadConnector(String id) {
            return Optional.empty();
        }

        @Override
        public Optional<Extension> loadExtension(String id) {
            return Optional.empty();
        }

        @Override
        public List<Extension> loadExtensionsByTag(String tag) {
            return Collections.emptyList();
        }

        @Override
        public Optional<InputStream> loadExtensionBLOB(String id) {
            return Optional.empty();
        }

        @Override
        public Optional<OpenApi> loadOpenApiDefinition(String id) {
            return Optional.ofNullable(integrationSupplier.getOpenApis().get(":" + id));
        }

        @Override
        public String decrypt(String encrypted) {
            return "secret";
        }
    }

    public String getName() {
        return name;
    }
}
