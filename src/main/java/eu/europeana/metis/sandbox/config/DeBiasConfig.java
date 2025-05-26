package eu.europeana.metis.sandbox.config;


import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateServiceImpl;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
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
   * @param datasetDeBiasRepository the detect repository
   * @param datasetRepository the dataset repository
   * @param recordDeBiasMainRepository the record de bias main repository
   * @param recordDeBiasDetailRepository the record de bias detail repository
   * @return the detect service
   */
  @Bean
  public DeBiasStateService debiasMachine(DatasetDeBiasRepository datasetDeBiasRepository,
      DatasetRepository datasetRepository,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository,
      ExecutionRecordRepository executionRecordRepository) {
    return new DeBiasStateServiceImpl(datasetDeBiasRepository,
        datasetRepository,
        recordDeBiasMainRepository,
        recordDeBiasDetailRepository,
        executionRecordRepository);
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

