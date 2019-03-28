package io.syndesis.qe.itest.integration.customizer;

import java.util.function.Function;

import io.syndesis.common.model.integration.Integration;

/**
 * @author Christoph Deppisch
 */
@FunctionalInterface
public interface IntegrationCustomizer extends Function<Integration, Integration> {
}
