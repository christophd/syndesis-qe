package io.syndesis.qe.itest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Christoph Deppisch
 */
public final class SyndesisTestEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger(SyndesisTestEnvironment.class);

    private static String syndesisVersion;

    private static final String SYNDESIS_PREFIX = "syndesis.";
    private static final String SYNDESIS_ENV_PREFIX = "SYNDESIS";

    /** System property names */
    private static final String SYNDESIS_VERSION = SYNDESIS_PREFIX + "version";
    private static final String SYNDESIS_VERSION_ENV = SYNDESIS_ENV_PREFIX + "VERSION";

    private static final String SYNDESIS_IMAGE_TAG = SYNDESIS_PREFIX + "image.tag";
    private static final String SYNDESIS_IMAGE_TAG_ENV = SYNDESIS_ENV_PREFIX + "IMAGE_TAG";

    private static final String S2I_BUILD_ENABLED = SYNDESIS_PREFIX + "s2i.build.enabled";
    private static final String S2I_BUILD_ENABLED_ENV = SYNDESIS_ENV_PREFIX + "S2I_BUILD_ENABLED";

    /* Load syndesis version */
    static {
        try (final InputStream in = new ClassPathResource("META-INF/syndesis.version").getInputStream()) {
            Properties versionProperties = new Properties();
            versionProperties.load(in);
            syndesisVersion = versionProperties.get("syndesis.version").toString();

            if (syndesisVersion.equals("${syndesis.version}")) {
                syndesisVersion = "1.7-SNAPSHOT";
            }
        } catch (IOException e) {
            LOG.warn("Unable to read syndesis version information", e);
            syndesisVersion = "1.7-SNAPSHOT";
        }
    }

    /**
     * Prevent instantiation of utility class.
     */
    private SyndesisTestEnvironment() {
        super();
    }

    public static String getSyndesisVersion() {
        return System.getProperty(SYNDESIS_VERSION, System.getenv(SYNDESIS_VERSION_ENV) != null ? System.getenv(SYNDESIS_VERSION_ENV) : syndesisVersion);
    }

    public static String getSyndesisImageTag() {
        String projectVersion = getSyndesisVersion();

        if (projectVersion.endsWith("SNAPSHOT")) {
            projectVersion = "latest";
        }

        return System.getProperty(SYNDESIS_IMAGE_TAG, System.getenv(SYNDESIS_IMAGE_TAG_ENV) != null ? System.getenv(SYNDESIS_IMAGE_TAG_ENV) : projectVersion);
    }

    public static boolean isS2iBuildEnabled() {
        return Boolean.parseBoolean(System.getProperty(S2I_BUILD_ENABLED, System.getenv(S2I_BUILD_ENABLED_ENV) != null ? System.getenv(S2I_BUILD_ENABLED_ENV) : "false"));
    }
}
