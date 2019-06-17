package io.syndesis.qe.itest.ftp;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.ftp.message.FtpMessage;
import io.syndesis.qe.itest.containers.integration.SyndesisIntegrationRuntimeContainer;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration(classes = FtpSplitToDB_IT.EndpointConfig.class)
@DirtiesContext
public class FtpSplitToDB_IT extends FtpTestSupport {

    /**
     * Integration periodically retrieves tasks as FTP file transfer and maps those to the database.
     * The integration uses a split step to pass entries one by one to the database.
     */
    @ClassRule
    public static SyndesisIntegrationRuntimeContainer integrationContainer = new SyndesisIntegrationRuntimeContainer.Builder()
            .name("ftp-split-to-db")
            .fromExport(FtpSplitToDB_IT.class.getResourceAsStream("FtpSplitToDB-export.zip"))
            .customize("$..configuredProperties.delay", "60000")
            .customize("$..configuredProperties.directoryName", "public")
            .customize("$..configuredProperties.fileName", "todo.json")
            .customize("$..configuredProperties.host", GenericContainer.INTERNAL_HOST_HOSTNAME)
            .customize("$..configuredProperties.port", ftpTestServerPort)
            .build()
            .withNetwork(getSyndesisDb().getNetwork());

    @Test
    @CitrusTest
    public void testFtpSplitToDB(@CitrusResource TestRunner runner) {
        runner.receive(receiveMessageBuilder -> receiveMessageBuilder
                .endpoint(ftpTestServer)
                .timeout(60000L)
                .message(FtpMessage.command(FTPCmd.RETR).arguments("todo.json")));

        runner.send(sendMessageBuilder -> sendMessageBuilder
                .endpoint(ftpTestServer)
                .message(FtpMessage.success()));

        runner.repeatOnError()
                .startsWith(1)
                .autoSleep(1000L)
                .until(Matchers.greaterThan(10))
                .actions(runner.query(builder -> builder.dataSource(sampleDb)
                        .statement("select count(*) as found_records from todo")
                        .validate("found_records", String.valueOf(3))));

        runner.query(builder -> builder.dataSource(sampleDb)
                .statement("select task, completed from todo")
                .validate("task", "FTP task #1", "FTP task #2", "FTP task #3")
                .validate("completed", "0", "1", "0"));
    }

    @Configuration
    public static class EndpointConfig {

        @Bean
        public DataConnectionConfiguration dataConnectionConfiguration() {
            DataConnectionConfigurationFactory dataConnectionFactory = new DataConnectionConfigurationFactory();
            dataConnectionFactory.setPassiveExternalAddress(integrationContainer.getInternalHostIp());
            dataConnectionFactory.setPassivePorts(String.valueOf(passivePort));
            return dataConnectionFactory.createDataConnectionConfiguration();
        }
    }
}
