package io.syndesis.qe.itest.integration.customizer;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.util.Json;

/**
 * @author Christoph Deppisch
 */
public class JsonPathIntegrationCustomizer implements IntegrationCustomizer {

    private final Map<String, Object> expressions;

    public JsonPathIntegrationCustomizer(String expression, Object value) {
        this(Collections.singletonMap(expression, value));
    }

    public JsonPathIntegrationCustomizer(Map<String, Object> expressions) {
        this.expressions = expressions;
    }

    @Override
    public Integration apply(Integration integration) {
        if (expressions.isEmpty()) {
            return integration;
        }

        try {
            final Configuration configuration = Configuration.builder()
                    .jsonProvider(new JacksonJsonProvider(Json.copyObjectMapperConfiguration()))
                    .mappingProvider(new JacksonMappingProvider(Json.copyObjectMapperConfiguration()))
                    .build();

            DocumentContext json = JsonPath.using(configuration).parse(Json.writer().forType(Integration.class).writeValueAsString(integration));
            for (Map.Entry<String, Object> expression : expressions.entrySet()) {
                json.set(expression.getKey(), expression.getValue());
            }

            return Json.reader().forType(Integration.class).readValue(json.jsonString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to evaluate json path expression on integration object", e);
        }
    }
}
