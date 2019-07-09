package io.syndesis.qe.itest.integration.source;

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
public class JsonIntegrationSource implements IntegrationSource {

    private final String json;

    public JsonIntegrationSource(String json) {
        this.json = json;
    }

    public JsonIntegrationSource(Path pathToJson) {
        try {
            this.json = Files.toString(pathToJson.toFile(), Charset.forName("utf-8"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to access integration file path", e);
        }
    }

    public JsonIntegrationSource(URL resource) {
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
