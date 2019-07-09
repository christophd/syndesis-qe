package io.syndesis.qe.itest.integration.project;

import java.nio.file.Path;

import io.syndesis.qe.itest.integration.source.IntegrationSource;

/**
 * @author Christoph Deppisch
 */
@FunctionalInterface
public interface ProjectBuilder {

    /**
     * Builds the integration project sources and provides the path to that project dir.
     * @param integrationSource
     * @return
     */
    Path build(IntegrationSource integrationSource);
}
