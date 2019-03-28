package io.syndesis.qe.itest.integration.supplier;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.model.openapi.OpenApi;

/**
 * @author Christoph Deppisch
 */
@FunctionalInterface
public interface IntegrationSupplier extends Supplier<Integration> {

    default Map<String, OpenApi> getOpenApis() {
        return Collections.emptyMap();
    }
}
