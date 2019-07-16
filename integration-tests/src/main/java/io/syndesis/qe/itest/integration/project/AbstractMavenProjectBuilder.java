package io.syndesis.qe.itest.integration.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import io.syndesis.qe.itest.SyndesisTestEnvironment;
import io.syndesis.qe.itest.integration.source.IntegrationSource;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractMavenProjectBuilder<T extends AbstractMavenProjectBuilder<T>> implements ProjectBuilder {

    private final String name;
    private final String syndesisVersion;

    private Path outputDir;
    private ProjectGeneratorConfiguration projectGeneratorConfiguration = new ProjectGeneratorConfiguration();
    private MavenProperties mavenProperties = new MavenProperties();

    public AbstractMavenProjectBuilder(String name, String syndesisVersion) {
        this.name = name;
        this.syndesisVersion = syndesisVersion;
        withOutputDirectory(SyndesisTestEnvironment.getOutputDirectory());
    }

    @Override
    public Path build(IntegrationSource source) {
        try {
            Path projectDir = Files.createTempDirectory(outputDir, name);
            ProjectGenerator projectGenerator = new ProjectGenerator(projectGeneratorConfiguration,
                    new StaticIntegrationResourceManager(source),
                    mavenProperties);
            try (TarArchiveInputStream in = new TarArchiveInputStream(projectGenerator.generate(source.get(), System.out::println))) {
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

            customizePomFile(source, projectDir.resolve("pom.xml"));
            customizeIntegrationFile(source, projectDir.resolve("src").resolve("main").resolve("resources").resolve("syndesis").resolve("integration").resolve("integration.json"));

            // auto add secrets and other integration test settings to application properties
            Files.write(projectDir.resolve("src").resolve("main").resolve("resources").resolve("application.properties"),
                    getApplicationProperties(source).getBytes(Charset.forName("utf-8")), StandardOpenOption.APPEND);
            return projectDir;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create integration project", e);
        }
    }

    protected void customizeIntegrationFile(IntegrationSource source, Path integrationFile) throws IOException {
    }

    protected void customizePomFile(IntegrationSource source, Path pomFile) throws IOException {
        // overwrite the syndesis version in generated pom.xml as project export may use another version as required in test.
        if (Files.exists(pomFile)) {
            List<String> pomLines = Files.readAllLines(pomFile, Charset.forName("utf-8"));
            StringBuilder newPom = new StringBuilder();
            for (String line : pomLines) {
                newPom.append(customizePomLine(line)).append(System.lineSeparator());
            }
            Files.write(pomFile, newPom.toString().getBytes(Charset.forName("utf-8")));
        }
    }

    protected String customizePomLine(String line) {
        if (line.trim().startsWith("<syndesis.version>") && line.trim().endsWith("</syndesis.version>")) {
            return line.substring(0, line.indexOf("<")) +
                    String.format("<syndesis.version>%s</syndesis.version>", syndesisVersion) +
                    line.substring(line.lastIndexOf(">") + 1);
        }

        return line;
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    private String getApplicationProperties(IntegrationSource source) throws IOException {
        ProjectGenerator projectGenerator = new ProjectGenerator(projectGeneratorConfiguration, new StaticIntegrationResourceManager(source), mavenProperties);
        Properties applicationProperties = customizeApplicationProperties(projectGenerator.generateApplicationProperties(source.get()));

        StringWriter writer = new StringWriter();
        applicationProperties.store(writer, "Auto added integration test properties");

        return writer.toString();
    }

    protected Properties customizeApplicationProperties(Properties applicationProperties) {
        applicationProperties.put("management.port", String.valueOf(SyndesisTestEnvironment.getManagementPort()));
        return applicationProperties;
    }

    public ProjectBuilder withOutputDirectory(String tmpDir) {
        try {
            this.outputDir = Files.createDirectories(Paths.get(tmpDir));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temp directory", e);
        }
        return self();
    }

    static class StaticIntegrationResourceManager implements IntegrationResourceManager {
        private final IntegrationSource source;

        StaticIntegrationResourceManager(IntegrationSource source) {
            this.source = source;
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
            return Optional.ofNullable(source.getOpenApis().get(":" + id));
        }

        @Override
        public String decrypt(String encrypted) {
            return "secret";
        }
    }

    /**
     * Obtains the name.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains the projectGeneratorConfiguration.
     *
     * @return
     */
    public ProjectGeneratorConfiguration getProjectGeneratorConfiguration() {
        return projectGeneratorConfiguration;
    }

    /**
     * Obtains the mavenProperties.
     *
     * @return
     */
    public MavenProperties getMavenProperties() {
        return mavenProperties;
    }
}
