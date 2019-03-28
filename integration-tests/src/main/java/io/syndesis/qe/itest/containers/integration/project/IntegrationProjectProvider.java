package io.syndesis.qe.itest.containers.integration.project;

import java.nio.file.Path;

import io.syndesis.qe.itest.integration.supplier.IntegrationSupplier;

/**
 * @author Christoph Deppisch
 */
@FunctionalInterface
public interface IntegrationProjectProvider {

    /**
     * Builds the integration project sources and provides the path to that project dir.
     * @param integrationSupplier
     * @return
     */
    Path buildProject(IntegrationSupplier integrationSupplier);
}
