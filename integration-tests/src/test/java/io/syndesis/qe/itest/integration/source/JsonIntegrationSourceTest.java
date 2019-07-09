package io.syndesis.qe.itest.integration.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.syndesis.common.model.action.ConnectorAction;
import io.syndesis.common.model.action.ConnectorDescriptor;
import io.syndesis.common.model.connection.Connection;
import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.integration.Flow;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.model.integration.Step;
import io.syndesis.common.model.integration.StepKind;
import io.syndesis.common.util.Json;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class JsonIntegrationSourceTest {

    @Test
    public void getFromJson() throws JsonProcessingException {
        Integration sample = new Integration.Builder()
                .name("test-integration")
                .addConnection(new Connection.Builder()
                    .name("test-connection")
                    .connector(new Connector.Builder()
                        .name("test-connector")
                        .addAction(new ConnectorAction.Builder()
                            .name("test-action")
                            .descriptor(new ConnectorDescriptor.Builder()
                                .componentScheme("test")
                                .build())
                            .build())
                        .build())
                    .build())
                .addFlow(new Flow.Builder()
                    .name("test-flow")
                    .addStep(new Step.Builder()
                        .stepKind(StepKind.log)
                        .putConfiguredProperty("customText", "Hello from Syndesis")
                        .build())
                    .build())
                .build();

        JsonIntegrationSource source = new JsonIntegrationSource(Json.writer().forType(Integration.class).writeValueAsString(sample));
        Assert.assertEquals(sample, source.get());
    }
}
