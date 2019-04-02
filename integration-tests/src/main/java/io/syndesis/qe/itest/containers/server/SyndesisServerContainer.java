package io.syndesis.qe.itest.containers.server;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import io.syndesis.qe.itest.SyndesisTestEnvironment;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Christoph Deppisch
 */
public class SyndesisServerContainer extends GenericContainer<SyndesisServerContainer> {

    private SyndesisServerContainer(String imageTag, String javaOptions) {
        super(String.format("syndesis/syndesis-server:%s", imageTag));
        withEnv("JAVA_OPTIONS", javaOptions);
    }

    private SyndesisServerContainer(String serverJarPath, String javaOptions, boolean deleteOnExit) {
        super(new ImageFromDockerfile("syndesis-server", deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from("fabric8/s2i-java:3.0-java8")
                             .env("JAVA_OPTIONS", javaOptions)
                             .expose(8080, 8778)
                             .build()));

        withClasspathResourceMapping(serverJarPath,"/deployments/server.jar", BindMode.READ_ONLY);
    }

    public static class Builder {
        private String imageTag = SyndesisTestEnvironment.getSyndesisImageTag();
        private boolean deleteOnExit = true;
        private boolean enableLogging = false;

        private String serverJarPath;

        private Map<String, String> javaOptions = new HashMap<>();

        public Builder() {
            javaOptions.put("encrypt.key", "supersecret");
            javaOptions.put("controllers.dblogging.enabled", "false");
            javaOptions.put("openshift.enabled", "false");
            javaOptions.put("metrics.kind", "noop");
            javaOptions.put("features.monitoring.enabled", "false");
            javaOptions.put("spring.datasource.url", "jdbc:postgresql://syndesis-db:5432/syndesis?sslmode=disable");
            javaOptions.put("spring.datasource.username", "syndesis");
            javaOptions.put("spring.datasource.password", "secret");
            javaOptions.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            javaOptions.put("dao.kind", "jsondb");
        }

        public SyndesisServerContainer build() {
            SyndesisServerContainer container;
            if (StringUtils.hasText(serverJarPath)) {
                container = new SyndesisServerContainer(serverJarPath, getJavaOptionString(), deleteOnExit);
            } else {
                container = new SyndesisServerContainer(imageTag, getJavaOptionString());
            }

            if (enableLogging) {
                container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("SERVER_CONTAINER")));
            }

            container.withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("syndesis-server"));
            container.withStartupTimeout(Duration.ofSeconds(180));
            container.withExposedPorts(8080, 8778);

            return container;
        }

        public SyndesisServerContainer.Builder withImageTag(String tag) {
            this.imageTag = tag;
            return this;
        }

        public SyndesisServerContainer.Builder withClasspathServerJar(String serverJarPath) {
            this.serverJarPath = serverJarPath;
            return this;
        }

        public SyndesisServerContainer.Builder withJavaOption(String name, String value) {
            this.javaOptions.put(name, value);
            return this;
        }

        public SyndesisServerContainer.Builder withDeleteOnExit(boolean deleteOnExit) {
            this.deleteOnExit = deleteOnExit;
            return this;
        }

        public SyndesisServerContainer.Builder enableLogging() {
            this.enableLogging = true;
            return this;
        }

        private String getJavaOptionString() {
            StringJoiner stringJoiner = new StringJoiner(" -D", "-D", "");
            stringJoiner.setEmptyValue("");
            javaOptions.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).forEach(stringJoiner::add);
            return stringJoiner.toString();
        }
    }

}
