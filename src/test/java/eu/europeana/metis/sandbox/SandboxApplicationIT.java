package eu.europeana.metis.sandbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
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
    PostgresContainerInitializerIT.dynamicProperties(registry);
    PostgresContainerInitializerIT.runScripts(
        List.of("database/schema_drop.sql", "database/schema.sql", "database/schema_problem_patterns_drop.sql",
            "database/schema_problem_patterns.sql"));
    RabbitMQContainerInitializerIT.properties(registry);
  }

  @Test
  void contextLoads(ApplicationContext applicationContext) {
    assertNotNull(applicationContext);
    assertNotNull(applicationContext.getBean(DataSource.class));
    assertNotNull(applicationContext.getBean(AmqpTemplate.class));
  }
}
