package io.syndesis.qe.itest.integration.supplier;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.util.Json;
import org.testcontainers.shaded.com.google.common.io.Files;

/**
 * @author Christoph Deppisch
 */
public class JsonIntegrationSupplier implements IntegrationSupplier {

    private final String json;

    public JsonIntegrationSupplier(String json) {
        this.json = json;
    }

    public JsonIntegrationSupplier(Path pathToJson) {
        try {
            this.json = Files.toString(pathToJson.toFile(), Charset.forName("utf-8"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to access integration file path", e);
        }
    }

    public JsonIntegrationSupplier(URL resource) {
        try {
            this.json = Files.toString(Paths.get(resource.toURI()).toFile(), Charset.forName("utf-8"));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to access integration file URL", e);
        }
    }

    @Override
    public Integration get() {
        try {
            return Json.reader().forType(Integration.class).readValue(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read integration json", e);
        }
    }
}
