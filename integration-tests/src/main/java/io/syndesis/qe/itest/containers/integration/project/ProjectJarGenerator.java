package io.syndesis.qe.itest.containers.integration.project;

import java.nio.file.Path;

import io.syndesis.qe.itest.integration.supplier.IntegrationSupplier;

/**
 * @author Christoph Deppisch
 */
@FunctionalInterface
public interface ProjectJarGenerator {

    /**
     * Builds the integration project as JAR and provides the path to that file.
     * @param integrationSupplier
     * @return
     */
    Path buildProjectJar(IntegrationSupplier integrationSupplier);
}
