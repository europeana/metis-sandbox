package eu.europeana.metis.sandbox.config.batch;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchJobConfig {

  @Bean
  public JobRegistry jobRegistry() {
    return new MapJobRegistry();
  }

  @Bean(name = "myJobOperator")
  public JobOperator jobOperator(
      JobRepository jobRepository,
      JobRegistry jobRegistry,
      PlatformTransactionManager transactionManager
  ) throws Exception {
    JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
    factory.setJobRepository(jobRepository);
    factory.setJobRegistry(jobRegistry);
    factory.setTransactionManager(transactionManager);
    factory.afterPropertiesSet();
    return factory.getObject();
  }
}

