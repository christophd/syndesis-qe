package io.syndesis.qe.itest.containers.server;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.util.StringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Christoph Deppisch
 */
public class SyndesisServerContainer extends GenericContainer<SyndesisServerContainer> {

    private SyndesisServerContainer(String syndesisVersion, String javaOptions) {
        super(String.format("syndesis/syndesis-server:%s", syndesisVersion));

        withEnv("JAVA_OPTIONS", javaOptions);
        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("syndesis-server"));
        withExposedPorts(8080);
    }

    private SyndesisServerContainer(String serverJarPath, String javaOptions, boolean deleteOnExit) {
        super(new ImageFromDockerfile("syndesis-server", deleteOnExit)
                .withDockerfileFromBuilder(builder -> builder.from("fabric8/s2i-java:3.0-java8")
                                                             .env("JAVA_APP_JAR", "server.jar")
                                                             .env("JAVA_OPTIONS", javaOptions)
                                                             .build()));

        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("syndesis-server"));

        withClasspathResourceMapping(serverJarPath,"/deployments/server.jar", BindMode.READ_ONLY);
        withExposedPorts(8080);
    }

    public static class Builder {
        private String syndesisVersion = "latest";
        private boolean deleteOnExit = true;

        private String serverJarPath;

        private Map<String, String> javaOptions = new HashMap<>();

        public Builder() {
            javaOptions.put("encrypt.key", "supersecret");
            javaOptions.put("controllers.dblogging.enabled", "false");
            javaOptions.put("openshift.enabled", "false");
            javaOptions.put("metrics.kind", "noop");
            javaOptions.put("features.monitoring.enabled", "false");
        }

        public SyndesisServerContainer build() {
            if (StringUtils.hasText(serverJarPath)) {
                return new SyndesisServerContainer(serverJarPath, getJavaOptionString(), deleteOnExit);
            } else {
                return new SyndesisServerContainer(syndesisVersion, getJavaOptionString());
            }
        }

        public SyndesisServerContainer.Builder withSyndesisVersion(String version) {
            this.syndesisVersion = version;
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

        private String getJavaOptionString() {
            String javaOptionString = "";
            if (javaOptions.isEmpty()) {
                StringJoiner stringJoiner = new StringJoiner("-D ");
                javaOptions.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).forEach(stringJoiner::add);
                javaOptionString = "-D" + stringJoiner.toString();
            }
            return javaOptionString;
        }
    }

}
