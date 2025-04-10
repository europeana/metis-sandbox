package eu.europeana.metis.sandbox.integration;

import static eu.europeana.metis.sandbox.test.utils.S3TestContainersConfiguration.BUCKET_NAME;
import static eu.europeana.metis.sandbox.test.utils.SolrTestContainersConfiguration.SOLR_COLLECTION_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import eu.europeana.metis.sandbox.test.utils.MongoTestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.RabbitMQTestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.S3TestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.SolrTestContainersConfiguration;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainersConfiguration.class, RabbitMQTestContainersConfiguration.class,
    MongoTestContainersConfiguration.class, SolrTestContainersConfiguration.class, S3TestContainersConfiguration.class})
class SandboxApplicationIT {
  // TODO: 27/06/2022 Mongo and Solr dbs should be fixed with relevant containers so that the
  //  eu.europeana.metis.sandbox.config.IndexingConfig.publishIndexer will not complain for IndexingServiceImpl

  // TODO: 28/06/2022 Github actions seems to download the docker image everytime, check if there is a cache that we can apply.

  @BeforeAll
  static void beforeAll() {
    //Sandbox specific properties
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.jdbcUrl", PostgreSQLContainer::getJdbcUrl);
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.username", PostgreSQLContainer::getUsername);
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.password", PostgreSQLContainer::getPassword);
    PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.driverClassName", container -> "org.postgresql.Driver");

    PostgresTestContainersConfiguration.runScripts(List.of(
        "database/schema_drop.sql", "database/schema.sql",
        "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql",
        "database/schema_lockrepository_drop.sql", "database/schema_lockrepository.sql",
        "database/schema_validation_drop.sql", "database/schema_validation.sql"
    ));

    //Sandbox specific datasource properties
    MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.application-name", container -> "mongo-testcontainer-test");
    MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.db", container -> "test");
    MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.hosts", MongoDBContainer::getHost);
    MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.ports", container -> container.getFirstMappedPort().toString());

    //Sandbox specific datasource properties
    SolrTestContainersConfiguration.dynamicProperty("sandbox.publish.solr.hosts",
        container -> "http://" + container.getHost() + ":" + container.getSolrPort() + "/solr/" + SOLR_COLLECTION_NAME);

    //Sandbox specific datasource properties
    S3TestContainersConfiguration.dynamicProperty("sandbox.s3.access-key", LocalStackContainer::getAccessKey);
    S3TestContainersConfiguration.dynamicProperty("sandbox.s3.secret-key", LocalStackContainer::getSecretKey);
    S3TestContainersConfiguration.dynamicProperty("sandbox.s3.endpoint", container -> container.getEndpointOverride(S3).toString());
    S3TestContainersConfiguration.dynamicProperty("sandbox.s3.signing-region", LocalStackContainer::getRegion);
    S3TestContainersConfiguration.dynamicProperty("sandbox.s3.thumbnails-bucket", container -> BUCKET_NAME);
  }

  @Test
  void contextLoads(ApplicationContext applicationContext) {
    assertNotNull(applicationContext);
    assertNotNull(applicationContext.getBean(DataSource.class));
    assertNotNull(applicationContext.getBean(AmqpTemplate.class));
  }
}
