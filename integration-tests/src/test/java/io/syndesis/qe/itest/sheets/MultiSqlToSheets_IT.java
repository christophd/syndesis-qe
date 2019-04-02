package io.syndesis.qe.itest.sheets;

import javax.servlet.Filter;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.dsl.runner.TestRunnerBeforeTestSupport;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.servlet.RequestCachingServletFilter;
import io.syndesis.qe.itest.SyndesisIntegrationTestSupport;
import io.syndesis.qe.itest.containers.integration.SyndesisIntegrationRuntimeContainer;
import io.syndesis.qe.itest.gzip.GzipServletFilter;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.SocketUtils;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration(classes = MultiSqlToSheets_IT.EndpointConfig.class)
public class MultiSqlToSheets_IT extends SyndesisIntegrationTestSupport {

    private static int googleSheetsServerPort = SocketUtils.findAvailableTcpPort();
    static {
        Testcontainers.exposeHostPorts(googleSheetsServerPort);
    }

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
            .withName("sql-to-sheets")
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

    @Configuration
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public static class EndpointConfig {

        @Bean
        public HttpServer googleSheetsApiServer() {
            Map<String, Filter> filterMap = new LinkedHashMap<>();
            filterMap.put("request-caching-filter", new RequestCachingServletFilter());
            filterMap.put("gzip-filter", new GzipServletFilter());

            return CitrusEndpoints.http()
                    .server()
                    .port(googleSheetsServerPort)
                    .autoStart(true)
                    .timeout(60000L)
                    .filters(filterMap)
                    .build();
        }

        @Bean
        public TestRunnerBeforeTestSupport beforeTest(DataSource sampleDb) {
            return new TestRunnerBeforeTestSupport() {
                @Override
                public void beforeTest(TestRunner runner) {
                    runner.sql(builder -> builder.dataSource(sampleDb)
                            .statement("delete from contact where lead_source='google-sheets'"));
                }
            };
        }
    }

}
