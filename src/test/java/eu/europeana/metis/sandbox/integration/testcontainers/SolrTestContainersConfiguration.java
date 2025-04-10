package eu.europeana.metis.sandbox.integration.testcontainers;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class SolrTestContainersConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String SOLR_VERSION = "solr:7.7.3-slim";
  private static final SolrContainer solrContainer;
  public static final String SOLR_COLLECTION_NAME = "solr_publish_test";

  static {
    solrContainer = new SolrContainer(DockerImageName.parse(SOLR_VERSION)).withCollection(SOLR_COLLECTION_NAME);
    solrContainer.start();
    setDynamicProperties();
    logConfiguration();
  }

  private static void setDynamicProperties() {
    System.setProperty("spring.data.solr.host", solrContainer.getHost());
    System.setProperty("spring.data.solr.port", String.valueOf(solrContainer.getSolrPort()));
  }

  public static void setDynamicProperty(String key, Function<SolrContainer, String> getValue){
    System.setProperty(key, getValue.apply(solrContainer));
  }

  private static void logConfiguration() {
    LOGGER.info("Solr container created:");
    LOGGER.info("Host: {}", solrContainer.getHost());
    LOGGER.info("Port: {}", solrContainer.getSolrPort());
  }
}
