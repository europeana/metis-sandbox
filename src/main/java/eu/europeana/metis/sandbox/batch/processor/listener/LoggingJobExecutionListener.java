package eu.europeana.metis.sandbox.batch.processor.listener;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Setter
public class LoggingJobExecutionListener implements JobExecutionListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void beforeJob(@NotNull JobExecution jobExecution) {
    LOGGER.info("Before job");
  }
  @Override
  public void afterJob(JobExecution jobExecution) {
    LOGGER.info("After job");
    LocalDateTime start = jobExecution.getCreateTime();
    LocalDateTime end = jobExecution.getEndTime();
    LOGGER.info("Total seconds to complete job: {}", ChronoUnit.SECONDS.between(start, end));
  }

}
