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
public class MultiSqlToSheets_IT extends SheetsIntegrationTestSupport {

    @Autowired
    private DataSource sampleDb;

    @Autowired
    private HttpServer googleSheetsApiServer;

    /**
     * Integration uses multiple data buckets for a data mapping. In this case mapper maps data from two SQL queries
     * and returns a contact list (first_name, company). This list is sent to Google Sheets API for appending the values
     * to a spreadsheet.
     */
    @ClassRule
    public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
            .withName("multi-sql-to-sheets")
            .fromExport(MultiSqlToSheets_IT.class.getResourceAsStream("MultiSqlToSheets-export.zip"))
            .customize("$..rootUrl.defaultValue",
                        String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, googleSheetsServerPort))
            .build()
            .withNetwork(getSyndesisDb().getNetwork());

    @Test
    @CitrusTest
    public void testMultiSqlMapper(@CitrusResource TestRunner runner) {
        runner.sql(builder -> builder.dataSource(sampleDb)
                .statements(Arrays.asList("insert into contact (first_name, last_name, company, lead_source) values ('Joe','Jackson','Red Hat','google-sheets')",
                                          "insert into contact (first_name, last_name, company, lead_source) values ('Joanne','Jackson','Red Hat','google-sheets')")));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                        .receive()
                        .post()
                        .payload("{\"majorDimension\":\"ROWS\",\"values\":[[\"Joe\",\"Red Hat\"],[\"Joanne\",\"Red Hat\"]]}"));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                        .send()
                        .response(HttpStatus.OK));
    }
}
