package io.syndesis.qe.itest.integration.supplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.model.openapi.OpenApi;
import io.syndesis.common.util.Json;

/**
 * @author Christoph Deppisch
 */
public class ExportIntegrationSupplier implements IntegrationSupplier {

    private final JsonNode model;

    public ExportIntegrationSupplier(InputStream export) {
        this.model = readModel(export);
    }

    public ExportIntegrationSupplier(Path pathToExport) {
        try {
            this.model = readModel(Files.newInputStream(pathToExport));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to access export file");
        }
    }

    @Override
    public Integration get() {
        try {
            JsonNode integrations = model.get("integrations");
            if (integrations != null && integrations.fields().hasNext()) {
                return Json.reader().forType(Integration.class).readValue(integrations.fields().next().getValue());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read integrations from export", e);
        }

        throw new IllegalStateException("Unable to import integration from export - no suitable integration source found");
    }

    @Override
    public Map<String, OpenApi> getOpenApis() {
        Map<String, OpenApi> openApis = new HashMap<>();

        try {
            JsonNode apis = model.get("open-apis");
            if (apis != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = apis.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> fieldEntry = fields.next();
                    openApis.put(fieldEntry.getKey(), Json.reader().forType(OpenApi.class).readValue(fieldEntry.getValue()));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read open apis from export", e);
        }

        return openApis;
    }

    private JsonNode readModel(InputStream export) {
        try (ZipInputStream zis = new ZipInputStream(export)) {
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if( entry == null ) {
                    break;
                }

                if ("model.json".equals(entry.getName())) {
                    return Json.reader().readTree(zis);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read integration export", e);
        }

        throw new IllegalStateException("Invalid export content - no integration model found");
    }
}
