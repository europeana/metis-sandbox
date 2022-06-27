package eu.europeana.metis.sandbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Sql(scripts = {"classpath:database/schema_drop.sql", "classpath:database/schema.sql"})
@Sql(scripts = {"classpath:database/schema_problem_patterns_drop.sql", "classpath:database/schema_problem_patterns.sql"})
class SandboxApplicationIT {
  // TODO: 27/06/2022 Mongo and Solr dbs should be fixed with relevant containers so that the
  //  eu.europeana.metis.sandbox.config.IndexingConfig.publishIndexer will not complain for IndexingServiceImpl

  // TODO: 27/06/2022 Fix the default xslt initialization in eu.europeana.metis.sandbox.scheduler.XsltUrlUpdateScheduler.init.
  // The configuration tries to insert to the db before the above Sql script it ran therefore it fails not finding the relevant table in the db

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    PostgresContainerInitializerIT.properties(registry);
    RabbitMQContainerInitializerIT.properties(registry);
  }

  @Test
  void contextLoads(ApplicationContext applicationContext) {
    assertNotNull(applicationContext);
    assertNotNull(applicationContext.getBean(DataSource.class));
    // TODO: 27/06/2022 Why are there two beans of AmqpTemplate.class?
    //    assertNotNull(applicationContext.getBean(AmqpTemplate.class));
    assertNotNull(applicationContext.getBean("amqpTemplate"));
    assertNotNull(applicationContext.getBean("rabbitTemplate"));

    System.out.println(applicationContext.getDisplayName());
    System.out.println(applicationContext.getId());

    System.out.println(applicationContext.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
    System.out.println(applicationContext.getEnvironment().getProperty("spring.datasource.url"));
    System.out.println(applicationContext.getEnvironment().getProperty("spring.datasource.driver-class-name"));
    System.out.println(applicationContext.getEnvironment().getProperty("spring.datasource.driverClassName"));
    System.out.println(applicationContext.getEnvironment().getProperty("sandbox.datasource.driverClassName"));
    System.out.println(applicationContext.getEnvironment().getProperty("sandbox.datasource.driverClassName"));
    System.out.println(applicationContext.getEnvironment().getProperty("sandbox.datasource.jdbcUrl"));

    System.out.println();
  }
}
