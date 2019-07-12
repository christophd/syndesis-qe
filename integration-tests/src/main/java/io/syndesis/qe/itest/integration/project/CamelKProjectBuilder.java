package io.syndesis.qe.itest.integration.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.syndesis.common.model.Dependency;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.integration.project.generator.ProjectGeneratorHelper;
import io.syndesis.integration.project.generator.mvn.MavenGav;
import io.syndesis.integration.project.generator.mvn.PomContext;
import io.syndesis.qe.itest.integration.source.IntegrationSource;

/**
 * @author Christoph Deppisch
 */
public class CamelKProjectBuilder extends AbstractMavenProjectBuilder {

    private final Mustache pomMustache;

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
        Files.write(pomFile, generatePom(source));
        super.customizePomFile(source, pomFile);
    }

    private byte[] generatePom(final IntegrationSource source) throws IOException {
        final Integration integration = source.get();
        final Set<MavenGav> dependencies = new StaticIntegrationResourceManager(source)
                .collectDependencies(integration).stream()
                .filter(Dependency::isMaven)
                .map(Dependency::getId)
                .map(MavenGav::new)
                .filter(ProjectGeneratorHelper::filterDefaultDependencies)
                .collect(Collectors.toCollection(TreeSet::new));

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
