package io.syndesis.qe.itest.containers.integration.project;

import java.nio.file.Path;

import io.syndesis.qe.itest.containers.s2i.SyndesisS2iContainer;
import io.syndesis.qe.itest.integration.supplier.IntegrationSupplier;

/**
 * @author Christoph Deppisch
 */
public class S2IProjectProvider extends ProjectProvider {

    private final String syndesisVersion;

    public S2IProjectProvider(String name, String syndesisVersion) {
        super(name);
        this.syndesisVersion = syndesisVersion;
    }

    @Override
    public Path buildProject(IntegrationSupplier integrationSupplier) {
        Path projectDir = super.buildProject(integrationSupplier);

        SyndesisS2iContainer syndesisS2iContainer = new SyndesisS2iContainer(getName(), projectDir, syndesisVersion);
        syndesisS2iContainer.start();

        return projectDir.resolve("target").resolve("project-0.1-SNAPSHOT.jar");
    }
}
