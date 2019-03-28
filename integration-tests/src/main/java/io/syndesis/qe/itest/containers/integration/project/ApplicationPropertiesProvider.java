package io.syndesis.qe.itest.containers.integration.project;

import io.syndesis.qe.itest.integration.supplier.IntegrationSupplier;

/**
 * @author Christoph Deppisch
 */
@FunctionalInterface
public interface ApplicationPropertiesProvider {

    /**
     * Generates application properties for given integration and provides
     * complete properties file content.
     *
     * @param integrationSupplier
     * @return
     */
    String getApplicationProperties(IntegrationSupplier integrationSupplier);
}
