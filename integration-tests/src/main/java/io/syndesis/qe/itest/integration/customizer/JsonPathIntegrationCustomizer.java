package io.syndesis.qe.itest.integration.customizer;

import java.io.IOException;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.util.Json;
import org.apache.camel.util.ObjectHelper;

/**
 * @author Christoph Deppisch
 */
public class JsonPathIntegrationCustomizer implements IntegrationCustomizer {

    private final String expression;
    private final String key;
    private final Object value;

    public JsonPathIntegrationCustomizer(String expression, Object value) {
        this(expression, null, value);
    }

    public JsonPathIntegrationCustomizer(String expression, String key, Object value) {
        this.expression = expression;
        this.key = key;
        this.value = value;
    }

    @Override
    public Integration apply(Integration integration) {
        if (ObjectHelper.isEmpty(expression)) {
            return integration;
        }

        try {
            final Configuration configuration = Configuration.builder()
                    .jsonProvider(new JacksonJsonProvider(Json.copyObjectMapperConfiguration()))
                    .mappingProvider(new JacksonMappingProvider(Json.copyObjectMapperConfiguration()))
                    .build();

            DocumentContext json = JsonPath.using(configuration).parse(Json.writer().forType(Integration.class).writeValueAsString(integration));

            if (ObjectHelper.isEmpty(key)) {
                json.set(expression, value);
            } else {
                json.put(expression, key, value);
            }

            return Json.reader().forType(Integration.class).readValue(json.jsonString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to evaluate json path expression on integration object", e);
        }
    }
}