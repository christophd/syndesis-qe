package io.syndesis.qe.itest.sheets;

import javax.servlet.Filter;
import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.dsl.runner.TestRunnerBeforeTestSupport;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.servlet.RequestCachingServletFilter;
import io.syndesis.qe.itest.SyndesisIntegrationTestSupport;
import io.syndesis.qe.itest.gzip.GzipServletFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.SocketUtils;
import org.testcontainers.Testcontainers;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration(classes = GoogleSheetsTestSupport.EndpointConfig.class)
public class GoogleSheetsTestSupport extends SyndesisIntegrationTestSupport {

    static int googleSheetsServerPort = SocketUtils.findAvailableTcpPort();
    static {
        Testcontainers.exposeHostPorts(googleSheetsServerPort);
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
