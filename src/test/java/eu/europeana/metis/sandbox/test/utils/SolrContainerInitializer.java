package eu.europeana.metis.sandbox.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

public class SolrContainerInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrContainerInitializer.class);
    static final SolrContainer solrContainer;
    public static final String SOLR_VERSION = "solr:9.0.0";
    public static final String SOLR_COLLECTION_NAME = "solr_publish_test";

    static {
        solrContainer = new SolrContainer(DockerImageName.parse(SOLR_VERSION))
                .withCollection(SOLR_COLLECTION_NAME);
        solrContainer.start();

        logConfiguration();
    }

    private static void logConfiguration() {
        LOGGER.info("Solr container created:");
        LOGGER.info("Host: {}", solrContainer.getHost());
        LOGGER.info("Port: {}", solrContainer.getSolrPort());

    }

    public static void dynamicProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.data.solr.port", solrContainer::getSolrPort);
        registry.add("spring.data.solr.host", solrContainer::getHost);

        String solrUrlHost = "http://" + solrContainer.getHost() + ":" + solrContainer.getSolrPort() + "/solr/" + SOLR_COLLECTION_NAME;

        // TODO: 13/09/2022 We should perhaps remove the specifics here and use the default spring configuration properties
        //Sandbox specific datasource properties
        registry.add("sandbox.publish.solr.hosts", () -> solrUrlHost);


    }

    private void close() {
        //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
    }
}
