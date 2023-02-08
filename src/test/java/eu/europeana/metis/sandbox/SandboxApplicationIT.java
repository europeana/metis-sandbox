package eu.europeana.metis.sandbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.europeana.metis.sandbox.test.utils.TestContainer;
import eu.europeana.metis.sandbox.test.utils.TestContainerFactoryIT;
import eu.europeana.metis.sandbox.test.utils.TestContainerType;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SandboxApplicationIT {
  // TODO: 27/06/2022 Mongo and Solr dbs should be fixed with relevant containers so that the
  //  eu.europeana.metis.sandbox.config.IndexingConfig.publishIndexer will not complain for IndexingServiceImpl

  // TODO: 28/06/2022 Github actions seems to download the docker image everytime, check if there is a cache that we can apply.

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    TestContainer postgresql = TestContainerFactoryIT.getContainer(TestContainerType.POSTGRES);
    postgresql.dynamicProperties(registry);
    postgresql.runScripts(List.of("database/schema_drop.sql", "database/schema.sql", "database/schema_problem_patterns_drop.sql",
        "database/schema_problem_patterns.sql",
        "database/schema_lockrepository_drop.sql", "database/schema_lockrepository.sql"));
    TestContainer rabbitMQ = TestContainerFactoryIT.getContainer(TestContainerType.RABBITMQ);
    rabbitMQ.dynamicProperties(registry);
    TestContainer mongoDBContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.MONGO);
    mongoDBContainerIT.dynamicProperties(registry);
    TestContainer solrContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.SOLR);
    solrContainerIT.dynamicProperties(registry);
    TestContainer s3ContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.S3);
    s3ContainerIT.dynamicProperties(registry);
  }

  @Test
  void contextLoads(ApplicationContext applicationContext) {
    assertNotNull(applicationContext);
    assertNotNull(applicationContext.getBean(DataSource.class));
    assertNotNull(applicationContext.getBean(AmqpTemplate.class));
  }
}
