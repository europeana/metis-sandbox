package eu.europeana.metis.sandbox.integration.testcontainers;

import static eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration.ACCESS_KEY;
import static eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration.REGION;
import static eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration.SECRET_KEY;
import static eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration.getEndpoint;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Helper class for setting dynamic properties for Testcontainers.
 * <p>
 * The static methods of this class are generally meant to be executed in {@link BeforeAll} methods so that after the required
 * containers are initialized the required properties are set in time and before the beans' autoconfiguration is finished.
 *
 * @see MongoTestContainersConfiguration
 * @see PostgresTestContainersConfiguration
 * @see S3TestContainersConfiguration
 * @see SolrTestContainersConfiguration
 */
public final class SandboxIntegrationConfiguration {

  private SandboxIntegrationConfiguration() {
    // private constructor
  }

  public static void testContainersConfiguration() {
    testContainersPostgresConfiguration();
    testContainersMongoConfiguration();
    testContainersSolrConfiguraiton();
    testContainersS3Configuration();
  }

  public static void testContainersPostgresConfiguration() {
    PostgresTestContainersConfiguration.runScripts(List.of(
        "schema_drop.sql", "schema.sql"
    ));
  }

  private static void testContainersMongoConfiguration() {
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.preview.mongo.application-name",
        container -> "mongo-testcontainer-test");
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.preview.mongo.db", container -> "test");
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.preview.mongo.hosts", MongoDBContainer::getHost);
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.preview.mongo.ports",
        container -> container.getFirstMappedPort().toString());
  }

  private static void testContainersSolrConfiguraiton() {
    SolrTestContainersConfiguration.setDynamicProperty("sandbox.preview.solr.hosts",
        container -> String.format("http://%s:%d/solr/%s", container.getHost(), container.getSolrPort(),
            SolrTestContainersConfiguration.SOLR_COLLECTION_NAME));
  }

  private static void testContainersS3Configuration() {
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.access-key", container -> ACCESS_KEY);
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.secret-key", container -> SECRET_KEY);
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.endpoint", container -> getEndpoint());
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.signing-region", container -> REGION);
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.thumbnails-bucket",
        container -> S3TestContainersConfiguration.BUCKET_NAME);
  }

}
