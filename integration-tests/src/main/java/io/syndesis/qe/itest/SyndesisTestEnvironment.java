package io.syndesis.qe.itest;

import io.syndesis.qe.itest.model.IntegrationRuntime;

/**
 * @author Christoph Deppisch
 */
public final class SyndesisTestEnvironment {

    private static final String SYNDESIS_PREFIX = "syndesis.";
    private static final String SYNDESIS_ENV_PREFIX = "SYNDESIS";

    /** System property names */
    private static final String SYNDESIS_VERSION_DEFAULT = "1.7-SNAPSHOT";
    private static final String SYNDESIS_VERSION = SYNDESIS_PREFIX + "version";
    private static final String SYNDESIS_VERSION_ENV = SYNDESIS_ENV_PREFIX + "VERSION";

    private static final String SYNDESIS_INTEGRATION_RUNTIME_DEFAULT = IntegrationRuntime.SPRING_BOOT.getId();
    private static final String SYNDESIS_INTEGRATION_RUNTIME = SYNDESIS_PREFIX + "integration.runtime";
    private static final String SYNDESIS_INTEGRATION_RUNTIME_ENV = SYNDESIS_ENV_PREFIX + "INTEGRATION_RUNTIME";

    private static final String SYNDESIS_CAMEL_K_CUSTOMIZERS_DEFAULT = "servlet,servletregistration,health,logging,syndesis";
    private static final String SYNDESIS_CAMEL_K_CUSTOMIZERS = SYNDESIS_PREFIX + "camel.k.customizers";
    private static final String SYNDESIS_CAMEL_K_CUSTOMIZERS_ENV = SYNDESIS_ENV_PREFIX + "CAMEL_K_CUSTOMIZERS";

    private static final String SYNDESIS_SERVER_PORT_DEFAULT = "8080";
    private static final String SYNDESIS_SERVER_PORT = SYNDESIS_PREFIX + "server.port";
    private static final String SYNDESIS_SERVER_PORT_ENV = SYNDESIS_ENV_PREFIX + "SERVER.PORT";

    private static final String SYNDESIS_MANAGEMENT_PORT_DEFAULT = "8081";
    private static final String SYNDESIS_MANAGEMENT_PORT = SYNDESIS_PREFIX + "management.port";
    private static final String SYNDESIS_MANAGEMENT_PORT_ENV = SYNDESIS_ENV_PREFIX + "MANAGEMENT.PORT";

    private static final String SYNDESIS_IMAGE_TAG_DEFAULT = "latest";
    private static final String SYNDESIS_IMAGE_TAG = SYNDESIS_PREFIX + "image.tag";
    private static final String SYNDESIS_IMAGE_TAG_ENV = SYNDESIS_ENV_PREFIX + "IMAGE_TAG";

    private static final String SYNDESIS_DEBUG_PORT_DEFAULT = "5005";
    private static final String SYNDESIS_DEBUG_PORT = SYNDESIS_PREFIX + "debug.port";
    private static final String SYNDESIS_DEBUG_PORT_ENV = SYNDESIS_ENV_PREFIX + "DEBUG_PORT";

    private static final String SYNDESIS_LOGGING_ENABLED = SYNDESIS_PREFIX + "logging.enabled";
    private static final String SYNDESIS_LOGGING_ENABLED_ENV = SYNDESIS_ENV_PREFIX + "LOGGING_ENABLED";

    private static final String SYNDESIS_DEBUG_ENABLED = SYNDESIS_PREFIX + "debug.enabled";
    private static final String SYNDESIS_DEBUG_ENABLED_ENV = SYNDESIS_ENV_PREFIX + "DEBUG_ENABLED";

    private static final String S2I_BUILD_ENABLED = SYNDESIS_PREFIX + "s2i.build.enabled";
    private static final String S2I_BUILD_ENABLED_ENV = SYNDESIS_ENV_PREFIX + "S2I_BUILD_ENABLED";

    private static final String SYNDESIS_OUTPUT_DIRECTORY_DEFAULT = "target/integrations";
    private static final String SYNDESIS_OUTPUT_DIRECTORY = SYNDESIS_PREFIX + "output.directory";
    private static final String SYNDESIS_OUTPUT_DIRECTORY_ENV = SYNDESIS_ENV_PREFIX + "OUTPUT_DIRECTORY";

    /**
     * Prevent instantiation of utility class.
     */
    private SyndesisTestEnvironment() {
        super();
    }

    public static String getSyndesisVersion() {
        return System.getProperty(SYNDESIS_VERSION, System.getenv(SYNDESIS_VERSION_ENV) != null ? System.getenv(SYNDESIS_VERSION_ENV) : SYNDESIS_VERSION_DEFAULT);
    }

    public static String getSyndesisImageTag() {
        String projectVersion = getSyndesisVersion();

        if (projectVersion.endsWith("SNAPSHOT")) {
            projectVersion = SYNDESIS_IMAGE_TAG_DEFAULT;
        }

        String imageTag = System.getProperty(SYNDESIS_IMAGE_TAG, System.getenv(SYNDESIS_IMAGE_TAG_ENV) != null ? System.getenv(SYNDESIS_IMAGE_TAG_ENV) : projectVersion);
        if (imageTag.isEmpty()) {
            imageTag = SYNDESIS_IMAGE_TAG_DEFAULT;
        }

        return imageTag;
    }

    public static int getServerPort() {
        return Integer.valueOf(System.getProperty(SYNDESIS_SERVER_PORT, System.getenv(SYNDESIS_SERVER_PORT_ENV) != null ? System.getenv(SYNDESIS_SERVER_PORT_ENV) : SYNDESIS_SERVER_PORT_DEFAULT));
    }

    public static int getManagementPort() {
        if (getIntegrationRuntime() == IntegrationRuntime.CAMEL_K) {
            return getServerPort();
        } else {
            return Integer.valueOf(System.getProperty(SYNDESIS_MANAGEMENT_PORT, System.getenv(SYNDESIS_MANAGEMENT_PORT_ENV) != null ? System.getenv(SYNDESIS_MANAGEMENT_PORT_ENV) : SYNDESIS_MANAGEMENT_PORT_DEFAULT));
        }
    }

    public static int getDebugPort() {
        return Integer.valueOf(System.getProperty(SYNDESIS_DEBUG_PORT, System.getenv(SYNDESIS_DEBUG_PORT_ENV) != null ? System.getenv(SYNDESIS_DEBUG_PORT_ENV) : SYNDESIS_DEBUG_PORT_DEFAULT));
    }

    public static String getOutputDirectory() {
        return System.getProperty(SYNDESIS_OUTPUT_DIRECTORY, System.getenv(SYNDESIS_OUTPUT_DIRECTORY_ENV) != null ? System.getenv(SYNDESIS_OUTPUT_DIRECTORY_ENV) : SYNDESIS_OUTPUT_DIRECTORY_DEFAULT);
    }

    public static boolean isLoggingEnabled() {
        return Boolean.valueOf(System.getProperty(SYNDESIS_LOGGING_ENABLED, System.getenv(SYNDESIS_LOGGING_ENABLED_ENV) != null ? System.getenv(SYNDESIS_LOGGING_ENABLED_ENV) : Boolean.FALSE.toString()));
    }

    public static boolean isDebugEnabled() {
        return Boolean.valueOf(System.getProperty(SYNDESIS_DEBUG_ENABLED, System.getenv(SYNDESIS_DEBUG_ENABLED_ENV) != null ? System.getenv(SYNDESIS_DEBUG_ENABLED_ENV) : Boolean.FALSE.toString()));
    }

    public static boolean isS2iBuildEnabled() {
        return Boolean.parseBoolean(System.getProperty(S2I_BUILD_ENABLED, System.getenv(S2I_BUILD_ENABLED_ENV) != null ? System.getenv(S2I_BUILD_ENABLED_ENV) : Boolean.FALSE.toString()));
    }

    public static IntegrationRuntime getIntegrationRuntime() {
        return IntegrationRuntime.fromId(System.getProperty(SYNDESIS_INTEGRATION_RUNTIME, System.getenv(SYNDESIS_INTEGRATION_RUNTIME_ENV) != null ? System.getenv(SYNDESIS_INTEGRATION_RUNTIME_ENV) : SYNDESIS_INTEGRATION_RUNTIME_DEFAULT));
    }

    public static String getCamelkCustomizers() {
        return System.getProperty(SYNDESIS_CAMEL_K_CUSTOMIZERS, System.getenv(SYNDESIS_CAMEL_K_CUSTOMIZERS_ENV) != null ? System.getenv(SYNDESIS_CAMEL_K_CUSTOMIZERS_ENV) : SYNDESIS_CAMEL_K_CUSTOMIZERS_DEFAULT);
    }
}