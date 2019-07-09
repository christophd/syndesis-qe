package io.syndesis.qe.itest.containers.s2i;

import java.nio.file.Path;

import io.syndesis.qe.itest.integration.project.ProjectBuilder;
import io.syndesis.qe.itest.integration.source.IntegrationSource;

/**
 * @author Christoph Deppisch
 */
public class S2iProjectBuilder implements ProjectBuilder {

    private final ProjectBuilder delegate;
    private final String imageTag;

    public S2iProjectBuilder(ProjectBuilder delegate, String imageTag) {
        this.delegate = delegate;
        this.imageTag = imageTag;
    }

    @Override
    public Path build(IntegrationSource source) {
        Path projectDir = delegate.build(source);

        SyndesisS2iAssemblyContainer syndesisS2iAssemblyContainer = new SyndesisS2iAssemblyContainer(projectDir.getFileName().toString(), projectDir, imageTag);
        syndesisS2iAssemblyContainer.start();

        return projectDir.resolve("target").resolve("project-0.1-SNAPSHOT.jar");
    }
}
