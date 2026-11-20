package eu.europeana.metis.sandbox.config.batch;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration class for spring batch job-related beans in the application context. This class defines and provides beans
 * required for job management.
 */
@Configuration
public class BatchJobConfig {

  /**
   * Provides a JobRegistry bean to manage the registration and retrieval of Spring Batch jobs within the application context.
   *
   * @return an instance of JobRegistry.
   */
  @Bean
  public JobRegistry jobRegistry() {
    return new MapJobRegistry();
  }

  /**
   * Registers Core-created job executions before returning while running the job on the pipeline executor. The UI-managed
   * workflow keeps using the synchronous operator below to sequence its steps.
   */
  @Bean(name = "engineJobOperator")
  public JobOperator engineJobOperator(JobRepository jobRepository, JobRegistry jobRegistry,
      PlatformTransactionManager transactionManager, @Qualifier("pipelineTaskExecutor") TaskExecutor taskExecutor)
      throws Exception {
    JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
    factory.setJobRepository(jobRepository);
    factory.setJobRegistry(jobRegistry);
    factory.setTransactionManager(transactionManager);
    factory.setTaskExecutor(taskExecutor);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

  /**
   * Configures and provides a {@link JobOperator} bean that facilitates the management and control of Spring Batch jobs,
   * including job execution and monitoring.
   * <p>
   * The UI-managed workflow uses this synchronous operator to run steps in sequence. {@link Primary} makes it the
   * default for unqualified {@link JobOperator} injection now that the asynchronous engine operator is also available.
   *
   * @param jobRepository the {@link JobRepository} used to retrieve and store job execution information.
   * @param jobRegistry the {@link JobRegistry} used to locate and register batch jobs in the application context.
   * @param transactionManager the {@link PlatformTransactionManager} used to manage transactions within job execution.
   * @return an instance of {@link JobOperator} configured with the provided dependencies.
   * @throws Exception if there is an issue during the creation or initialization of the {@link JobOperator} bean.
   */
  @Primary
  @Bean
  public JobOperator synchronousJobOperator(
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

