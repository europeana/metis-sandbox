package eu.europeana.metis.sandbox.config.batch;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "sandbox")
public record WorkflowConfigurationProperties(Map<BatchJobType, ParallelizeConfig> workflow) {

  public record ParallelizeConfig(int chunkSize, int parallelizeSize) {
  }
}
