package io.syndesis.qe.itest.integration.project;

/**
 * @author Christoph Deppisch
 */
public class SpringBootProjectBuilder extends AbstractMavenProjectBuilder<SpringBootProjectBuilder> {

    public SpringBootProjectBuilder(String name, String syndesisVersion) {
        super(name, syndesisVersion);
    }
}
