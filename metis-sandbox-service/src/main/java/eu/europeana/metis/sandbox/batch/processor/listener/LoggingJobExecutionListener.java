package eu.europeana.metis.sandbox.batch.processor.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Listener for logging events during the execution of a Spring Batch job.
 *
 * <p>Logs details before and after a job's execution for monitoring and debugging purposes.
 * <p>Provides the total execution time of the job upon completion.
 */
@Slf4j
@Component
public class LoggingJobExecutionListener implements JobExecutionListener {

  @Override
  public void beforeJob(@NotNull JobExecution jobExecution) {
    log.info("Before job");
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    log.info("After job");

    LocalDateTime start = jobExecution.getStartTime();
    LocalDateTime end = jobExecution.getEndTime();

    if (start != null && end != null) {
      long seconds = Duration.between(
          start.toInstant(ZoneOffset.UTC),
          end.toInstant(ZoneOffset.UTC)
      ).toSeconds();

      log.info("Total seconds to complete job: {}", seconds);
    }
  }
}
