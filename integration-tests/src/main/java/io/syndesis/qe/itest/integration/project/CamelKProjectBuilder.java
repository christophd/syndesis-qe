package io.syndesis.qe.itest.integration.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.syndesis.common.model.Dependency;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.model.integration.IntegrationDeployment;
import io.syndesis.integration.api.IntegrationResourceManager;
import io.syndesis.integration.project.generator.ProjectGeneratorHelper;
import io.syndesis.integration.project.generator.mvn.MavenGav;
import io.syndesis.integration.project.generator.mvn.PomContext;
import io.syndesis.qe.itest.SyndesisTestEnvironment;
import io.syndesis.qe.itest.integration.source.IntegrationSource;
import io.syndesis.server.controller.ControllersConfigurationProperties;
import io.syndesis.server.controller.integration.camelk.CamelKSupport;
import io.syndesis.server.controller.integration.camelk.crd.DataSpec;
import io.syndesis.server.controller.integration.camelk.crd.ResourceSpec;
import io.syndesis.server.controller.integration.camelk.crd.SourceSpec;
import io.syndesis.server.controller.integration.camelk.customizer.CamelKIntegrationCustomizer;
import io.syndesis.server.controller.integration.camelk.customizer.DependenciesCustomizer;
import io.syndesis.server.controller.integration.camelk.customizer.ExposureCustomizer;
import io.syndesis.server.controller.integration.camelk.customizer.OpenApiCustomizer;
import io.syndesis.server.controller.integration.camelk.customizer.TemplatingCustomizer;
import io.syndesis.server.controller.integration.camelk.customizer.WebhookCustomizer;
import io.syndesis.server.endpoint.v1.VersionService;
import io.syndesis.server.openshift.Exposure;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 */
public class CamelKProjectBuilder extends AbstractMavenProjectBuilder {

    private static final String MAVEN_GAV_PREFIX = "mvn:";

    private final Mustache pomMustache;
    private final VersionService versionService = new VersionService();

    public CamelKProjectBuilder(String name, String syndesisVersion) {
        super(name, syndesisVersion);

        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        try (InputStream stream = CamelKProjectBuilder.class.getResource("template/pom.xml.mustache").openStream()) {
            this.pomMustache = mustacheFactory.compile(new InputStreamReader(stream, StandardCharsets.UTF_8), name);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Camel-K pom template file", e);
        }
    }

    @Override
    protected void customizeIntegrationFile(IntegrationSource source, Path integrationFile) throws IOException {
        Path syndesisIntegrationFile = integrationFile.getParent().resolve("integration.syndesis");
        Files.copy(integrationFile, syndesisIntegrationFile);
        super.customizeIntegrationFile(source, syndesisIntegrationFile);
    }

    @Override
    protected void customizePomFile(IntegrationSource source, Path pomFile) throws IOException {
        Files.write(pomFile, generatePom(source, pomFile));
        super.customizePomFile(source, pomFile);
    }

    @Override
    protected String customizePomLine(String line) {
        if (line.trim().startsWith("<camel.version>") && line.trim().endsWith("</camel.version>")) {
            return line.substring(0, line.indexOf("<")) +
                    String.format("<camel.version>%s</camel.version>", versionService.getCamelVersion()) +
                    line.substring(line.lastIndexOf(">") + 1);
        }

        if (line.trim().startsWith("<camel-k-runtime.version>") && line.trim().endsWith("</camel-k-runtime.version>")) {
            return line.substring(0, line.indexOf("<")) +
                    String.format("<camel-k-runtime.version>%s</camel-k-runtime.version>", versionService.getCamelkRuntimeVersion()) +
                    line.substring(line.lastIndexOf(">") + 1);
        }

        return super.customizePomLine(line);
    }

    private byte[] generatePom(final IntegrationSource source, Path pomFile) throws IOException {
        final Integration integration = source.get();
        final Set<MavenGav> dependencies = new StaticIntegrationResourceManager(source)
                .collectDependencies(integration).stream()
                .filter(Dependency::isMaven)
                .map(Dependency::getId)
                .map(MavenGav::new)
                .filter(ProjectGeneratorHelper::filterDefaultDependencies)
                .collect(Collectors.toCollection(TreeSet::new));

        ControllersConfigurationProperties configurationProperties = new ControllersConfigurationProperties();
        IntegrationResourceManager integrationResourceManager = new StaticIntegrationResourceManager(() -> integration);
        IntegrationDeployment integrationDeployment = new IntegrationDeployment.Builder().spec(integration).build();
        EnumSet<Exposure> exposures = CamelKSupport.determineExposure(configurationProperties, integrationDeployment);

        List<CamelKIntegrationCustomizer> customizers = Arrays.asList(
                new TemplatingCustomizer(),
                new OpenApiCustomizer(configurationProperties, integrationResourceManager),
                new WebhookCustomizer(),
                new ExposureCustomizer(),
                new DependenciesCustomizer(versionService, integrationResourceManager));

        io.syndesis.server.controller.integration.camelk.crd.Integration integrationCR = new io.syndesis.server.controller.integration.camelk.crd.Integration();
        for (CamelKIntegrationCustomizer customizer : customizers) {
            integrationCR = customizer.customize(integrationDeployment, integrationCR, exposures);
        }

        for (String dependency : integrationCR.getSpec().getDependencies()) {
            if (dependency.contains("/")) {
                String version = null;
                String[] coordinates = dependency.split("/");
                if (coordinates.length > 1) {
                    String groupId = coordinates[0];
                    if (groupId.startsWith(MAVEN_GAV_PREFIX)) {
                        groupId = groupId.substring(MAVEN_GAV_PREFIX.length());
                        String artifactId = coordinates[1];

                        if (coordinates.length > 2) {
                            version = coordinates[2];
                        }

                        MavenGav gav = new MavenGav(groupId, artifactId, version);
                        if (dependencies.stream()
                                .noneMatch(p -> gav.getGroupId().equals(p.getGroupId()) &&
                                                gav.getArtifactId().equals(p.getArtifactId()))) {
                            dependencies.add(gav);
                        }
                    }
                }
            } else if (dependency.startsWith(MAVEN_GAV_PREFIX) && !dependency.contains(":bom:")) {
                MavenGav gav = new MavenGav(dependency.substring(MAVEN_GAV_PREFIX.length()));
                if (dependencies.stream()
                        .noneMatch(p -> gav.getGroupId().equals(p.getGroupId()) &&
                                        gav.getArtifactId().equals(p.getArtifactId()))) {
                    dependencies.add(gav);
                }
            }

            if (dependencies.stream().anyMatch(p -> p.getArtifactId().equals("camel-k-runtime-servlet"))) {
                dependencies.add(new MavenGav("org.apache.camel", "camel-servlet", versionService.getCamelVersion()));
            }
        }

        for (ResourceSpec resourceSpec : integrationCR.getSpec().getResources()) {
                DataSpec resourceData = resourceSpec.getDataSpec();
                if (resourceData != null && resourceData.getContent() != null) {
                    Files.write(pomFile.getParent().resolve("src").resolve("main").resolve("resources").resolve(resourceData.getName()),
                            resourceData.getContent().getBytes(Charset.forName("utf-8")), StandardOpenOption.CREATE);
                }
        }

        for (SourceSpec sourceSpec : integrationCR.getSpec().getSources()) {
                DataSpec sourceData = sourceSpec.getDataSpec();
                if (sourceData != null && sourceData.getContent() != null) {
                    Files.write(pomFile.getParent().resolve("src").resolve("main").resolve("resources").resolve(sourceData.getName() + "." + sourceSpec.getLanguage()),
                            sourceData.getContent().getBytes(Charset.forName("utf-8")), StandardOpenOption.CREATE);
                }
        }

        StringBuilder propertiesBuilder = new StringBuilder();
        integrationCR.getSpec().getConfiguration().stream().filter(configurationSpec -> "property".equals(configurationSpec.getType())).forEach(configurationSpec -> {
            propertiesBuilder.append(configurationSpec.getValue()).append(System.lineSeparator());
        });

        for (String customizer : SyndesisTestEnvironment.getCamelkCustomizers()) {
            propertiesBuilder.append("customizer.").append(customizer).append(".enabled=true").append(System.lineSeparator());
        }

        if (StringUtils.hasText(propertiesBuilder.toString())) {
            // auto add integration custom resource configuration to application properties
            Files.write(pomFile.getParent().resolve("src").resolve("main").resolve("resources").resolve("application.properties"),
                    propertiesBuilder.toString().getBytes(Charset.forName("utf-8")), StandardOpenOption.APPEND);
        }

        return ProjectGeneratorHelper.generate(
                new PomContext(
                        integration.getId().orElse(""),
                        integration.getName(),
                        integration.getDescription().orElse(null),
                        dependencies,
                        getMavenProperties()),
                pomMustache
        );
    }
}
