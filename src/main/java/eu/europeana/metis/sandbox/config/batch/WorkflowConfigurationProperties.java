package eu.europeana.metis.sandbox.config.batch;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for defining workflow settings based on batch job types.
 *
 * <p>Allows configuration of chunk size and parallelization for each specific
 * {@code BatchJobType} through a mapping to {@code ParallelizeConfig}.
 */
@ConfigurationProperties(prefix = "sandbox")
public record WorkflowConfigurationProperties(Map<BatchJobType, ParallelizeConfig> workflow) {

  /**
   * Represents the configuration for batch job parallelization.
   *
   * <p>Defines the chunk size and the level of parallelization used for processing
   * batch jobs in a multithreaded environment.
   *
   * @param chunkSize Defines the size of each chunk processed in a single step.
   * @param parallelizeSize Specifies the maximum number of threads used for parallel processing.
   */
  public record ParallelizeConfig(int chunkSize, int parallelizeSize) {

  }
}
