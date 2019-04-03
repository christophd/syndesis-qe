package io.syndesis.qe.itest.sheets;

import javax.sql.DataSource;
import java.util.Arrays;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.http.server.HttpServer;
import io.syndesis.qe.itest.containers.integration.SyndesisIntegrationRuntimeContainer;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;

/**
 * @author Christoph Deppisch
 */
public class DBSplitToSheets_IT extends SheetsIntegrationTestSupport {

    @Autowired
    private DataSource sampleDb;

    @Autowired
    private HttpServer googleSheetsApiServer;

    /**
     * Integration periodically retrieves all contacts from the database and maps the entries (first_name, last_name, company) to a spreadsheet on a Google Sheets account.
     * The integration uses a split step to pass entries one by one to the Google Sheets API.
     */
    @ClassRule
    public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
            .withName("db-split-to-sheets")
            .fromExport(DBSplitToSheets_IT.class.getResourceAsStream("DBSplitToSheets-export.zip"))
            .customize("$..rootUrl.defaultValue",
                        String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, googleSheetsServerPort))
            .build()
            .withNetwork(getSyndesisDb().getNetwork());

    @Test
    @CitrusTest
    public void testDBSplitToSheets(@CitrusResource TestRunner runner) {
        runner.sql(builder -> builder.dataSource(sampleDb)
                .statements(Arrays.asList("insert into contact (first_name, last_name, company, lead_source) values ('Joe','Jackson','Red Hat','google-sheets')",
                                          "insert into contact (first_name, last_name, company, lead_source) values ('Joanne','Jackson','Red Hat','google-sheets')")));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                        .receive()
                        .put()
                        .payload("{\"majorDimension\":\"ROWS\",\"values\":[[\"Joe\",\"Jackson\",\"Red Hat\"]]}"));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                        .send()
                        .response(HttpStatus.OK));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                .receive()
                .put()
                .payload("{\"majorDimension\":\"ROWS\",\"values\":[[\"Joanne\",\"Jackson\",\"Red Hat\"]]}"));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                .send()
                .response(HttpStatus.OK));
    }
}
