package io.syndesis.qe.itest.integration.source;

import java.util.List;
import java.util.Map;

import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.model.openapi.OpenApi;
import io.syndesis.qe.itest.integration.customizer.IntegrationCustomizer;

/**
 * @author Christoph Deppisch
 */
public class CustomizedIntegrationSource implements IntegrationSource {

    private final IntegrationSource delegate;
    private final List<IntegrationCustomizer> customizers;

    public CustomizedIntegrationSource(IntegrationSource delegate, List<IntegrationCustomizer> customizers) {
        this.delegate = delegate;
        this.customizers = customizers;
    }

    @Override
    public Integration get() {
        Integration integration = delegate.get();
        for (IntegrationCustomizer customizer : customizers) {
            integration = customizer.apply(integration);
        }

        return integration;
    }

    @Override
    public Map<String, OpenApi> getOpenApis() {
        return delegate.getOpenApis();
    }
}
