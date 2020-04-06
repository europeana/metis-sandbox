package eu.europeana.metis.sandbox.service.validation;

import eu.europeana.validation.service.ValidationExecutionService;
import eu.europeana.validation.service.ValidationServiceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This implementation is required by {@link ValidationExecutionService}
 */
@Service
class ValidationServiceConfigImpl implements ValidationServiceConfig {

  @Value("${sandbox.validation.executor.pool-size}")
  private Integer threadCount;

  @Override
  public int getThreadCount() {
    return threadCount;
  }
}
