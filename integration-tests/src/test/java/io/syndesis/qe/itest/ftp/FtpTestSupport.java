package io.syndesis.qe.itest.ftp;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.dsl.runner.TestRunnerBeforeTestSupport;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.ftp.client.FtpEndpointConfiguration;
import com.consol.citrus.ftp.server.FtpServer;
import io.syndesis.qe.itest.SyndesisIntegrationTestSupport;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.listener.ListenerFactory;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;
import org.testcontainers.Testcontainers;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration(classes = FtpTestSupport.EndpointConfig.class)
public abstract class FtpTestSupport extends SyndesisIntegrationTestSupport {

    static int ftpTestServerPort = SocketUtils.findAvailableTcpPort();
    static int passivePort = SocketUtils.findAvailableTcpPort(35000);
    static {
        Testcontainers.exposeHostPorts(ftpTestServerPort);
        Testcontainers.exposeHostPorts(passivePort);
    }

    @Autowired
    protected DataSource sampleDb;

    @Autowired
    protected FtpServer ftpTestServer;

    @Configuration
    public static class EndpointConfig {

        @Bean
        public FtpServer ftpTestServer(DataConnectionConfiguration dataConnectionConfiguration) {
            FtpEndpointConfiguration endpointConfiguration = new FtpEndpointConfiguration();
            endpointConfiguration.setAutoConnect(true);
            endpointConfiguration.setAutoLogin(true);
            endpointConfiguration.setAutoHandleCommands(
                    String.join(",", FTPCmd.PORT.getCommand(),
                            FTPCmd.MKD.getCommand(),
                            FTPCmd.PWD.getCommand(),
                            FTPCmd.CWD.getCommand(),
                            FTPCmd.PASV.getCommand(),
                            FTPCmd.NOOP.getCommand(),
                            FTPCmd.SYST.getCommand(),
                            FTPCmd.LIST.getCommand(),
                            FTPCmd.QUIT.getCommand(),
                            FTPCmd.TYPE.getCommand()));
            endpointConfiguration.setPort(ftpTestServerPort);

            FtpServer ftpServer = new FtpServer(endpointConfiguration);
            ftpServer.setUserManagerProperties(new ClassPathResource("ftp.server.properties", FtpTestSupport.class));
            ftpServer.setAutoStart(true);

            ListenerFactory listenerFactory = new ListenerFactory();
            listenerFactory.setDataConnectionConfiguration(dataConnectionConfiguration);
            ftpServer.setListenerFactory(listenerFactory);

            return ftpServer;
        }

        @Bean
        public TestRunnerBeforeTestSupport beforeTest(DataSource sampleDb) {
            return new TestRunnerBeforeTestSupport() {
                @Override
                public void beforeTest(TestRunner runner) {
                    runner.sql(builder -> builder.dataSource(sampleDb)
                            .statement("delete from todo"));
                }
            };
        }
    }

    @BeforeClass
    public static void setupFtpUserHome() {
        try {
            Path ftpUserHome = Paths.get("target/ftp/user/syndesis/public");
            if (!ftpUserHome.toFile().exists() && !ftpUserHome.toFile().mkdirs()) {
                throw new CitrusRuntimeException("Failed to setup ftp user home directory");
            }

            File todoFile = ftpUserHome.resolve("todo.json").toFile();
            if (!todoFile.exists()) {
                FileCopyUtils.copy(new ClassPathResource("todo.json", FtpToDB_IT.class).getFile(), todoFile);
            }
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to add files to ftp user home directory", e);
        }
    }
}
