package eu.europeana.metis.sandbox.integration.testcontainers;

import static eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration.BUCKET_NAME;
import static eu.europeana.metis.sandbox.integration.testcontainers.SolrTestContainersConfiguration.SOLR_COLLECTION_NAME;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

/**
 * Helper class for setting dynamic properties for Testcontainers.
 * <p>
 * The static methods of this class are generally meant to be executed in {@link BeforeAll} methods so that after the required
 * containers are initialized the required properties are set in time and before the beans' autoconfiguration is finished.
 * @see MongoTestContainersConfiguration
 * @see PostgresTestContainersConfiguration
 * @see RabbitMQTestContainersConfiguration
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
        "database/schema_drop.sql", "database/schema.sql",
        "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql",
        "database/schema_lockrepository_drop.sql", "database/schema_lockrepository.sql",
        "database/schema_validation_drop.sql", "database/schema_validation.sql"
    ));
  }

  private static void testContainersMongoConfiguration() {
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.publish.mongo.application-name",
        container -> "mongo-testcontainer-test");
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.publish.mongo.db", container -> "test");
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.publish.mongo.hosts", MongoDBContainer::getHost);
    MongoTestContainersConfiguration.setDynamicProperty("sandbox.publish.mongo.ports",
        container -> container.getFirstMappedPort().toString());
  }

  private static void testContainersSolrConfiguraiton() {
    SolrTestContainersConfiguration.setDynamicProperty("sandbox.publish.solr.hosts",
        container -> String.format("http://%s:%d/solr/%s", container.getHost(), container.getSolrPort(), SOLR_COLLECTION_NAME));
  }

  private static void testContainersS3Configuration() {
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.access-key", LocalStackContainer::getAccessKey);
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.secret-key", LocalStackContainer::getSecretKey);
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.endpoint",
        container -> container.getEndpointOverride(S3).toString());
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.signing-region", LocalStackContainer::getRegion);
    S3TestContainersConfiguration.setDynamicProperty("sandbox.s3.thumbnails-bucket", container -> BUCKET_NAME);
  }

}
