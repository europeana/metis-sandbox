package eu.europeana.metis.sandbox.config;


import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.debias.DetectRepository;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import eu.europeana.metis.sandbox.service.debias.DetectService;
import eu.europeana.metis.sandbox.service.debias.RecordDeBiasPublishable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type DeBias config.
 */
@Configuration
public class DeBiasConfig {

  @Value("${sandbox.debias.url}")
  private String apiUrl;

  @Value("${sandbox.debias.connectTimeout}")
  private int connectTimeout;

  @Value("${sandbox.debias.requestTimeout}")
  private int requestTimeout;

  /**
   * DeBias machine detect service.
   *
   * @param detectRepository the detect repository
   * @param datasetRepository the dataset repository
   * @param recordLogRepository the record log repository
   * @param recordDeBiasPublishable the record publishable
   * @return the detect service
   */
  @Bean
  public DetectService debiasMachine(DetectRepository detectRepository,
      DatasetRepository datasetRepository,
      RecordLogRepository recordLogRepository,
      RecordDeBiasPublishable recordDeBiasPublishable) {
    return new DeBiasStateService(detectRepository, datasetRepository, recordLogRepository, recordDeBiasPublishable);
  }

  /**
   * DeBias client 
   *
   * @return the DeBias client
   */
  @Bean
  public DeBiasClient deBiasClient() {
    return new DeBiasClient(apiUrl, connectTimeout, requestTimeout);
  }
}

