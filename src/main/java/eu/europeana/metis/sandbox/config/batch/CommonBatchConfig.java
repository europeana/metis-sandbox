package eu.europeana.metis.sandbox.config.batch;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class CommonBatchConfig {

  public static final int JOB_LAUNCHER_CORE_POOL_SIZE = 10;
  public static final int JOB_LAUNCHER_MAX_POOL_SIZE = 20;
  public static final int JOB_LAUNCHER_POOL_CAPACITY = 100;

  /**
   * Creates and configures an asynchronous {@link JobLauncher} bean with a task executor for parallel processing.
   *
   * <p>In the current implementation this does not matter much since we handle the workflow execution with the task executor
   * that is defined in the main configuration.
   *
   * @param jobRepository the job repository required by the {@link JobLauncher}
   * @return a fully configured asynchronous {@link JobLauncher}
   * @throws Exception if there is an issue while initializing the job launcher
   */
  @Bean("asyncJobLauncher")
  JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
    TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setJobRepository(jobRepository);

    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(JOB_LAUNCHER_CORE_POOL_SIZE);
    threadPoolTaskExecutor.setMaxPoolSize(JOB_LAUNCHER_MAX_POOL_SIZE);
    threadPoolTaskExecutor.setQueueCapacity(JOB_LAUNCHER_POOL_CAPACITY);
    threadPoolTaskExecutor.initialize();

    jobLauncher.setTaskExecutor(threadPoolTaskExecutor);
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }
}
