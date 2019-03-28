package io.syndesis.qe.itest;

import javax.sql.DataSource;

import com.consol.citrus.dsl.junit.JUnit4CitrusTest;
import io.syndesis.qe.itest.containers.db.SyndesisDbContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration(classes = SyndesisIntegrationTestSupport.EndpointConfig.class)
public abstract class SyndesisIntegrationTestSupport extends JUnit4CitrusTest {

    private static SyndesisDbContainer syndesisDb;

    static {
        syndesisDb = new SyndesisDbContainer();
        syndesisDb.start();
    }

    @Configuration
    public static class EndpointConfig {
        @Bean
        public DataSource sampleDb() {
            return new SingleConnectionDataSource(syndesisDb.getJdbcUrl(),
                                                    syndesisDb.getUsername(),
                                                    syndesisDb.getPassword(), true);
        }

        @Bean
        public DataSource syndesisDb() {
            return new SingleConnectionDataSource(syndesisDb.getJdbcUrl(),
                                                    "syndesis",
                                                    syndesisDb.getPassword(), true);
        }
    }

    protected static SyndesisDbContainer getSyndesisDb() {
        return syndesisDb;
    }
}
