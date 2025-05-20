package eu.europeana.metis.sandbox.batch.common;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;

/**
 * Adds a timestamp to the job so that it can be re-run with the same parameters.
 */
public class TimestampJobParametersIncrementer implements JobParametersIncrementer {

  @Override
  public @NotNull JobParameters getNext(JobParameters parameters) {
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder(parameters);
    jobParametersBuilder.addLong("timestamp", System.currentTimeMillis());
    return jobParametersBuilder.toJobParameters();
  }
}