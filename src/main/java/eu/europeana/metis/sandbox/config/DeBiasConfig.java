package eu.europeana.metis.sandbox.config;


import eu.europeana.metis.debias.detect.client.DeBiasClient;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.debias.DatasetDeBiasRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasDetailRepository;
import eu.europeana.metis.sandbox.repository.debias.RecordDeBiasMainRepository;
import eu.europeana.metis.sandbox.service.debias.DeBiasStateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DeBiasConfig {

  @Value("${sandbox.debias.url}")
  private String apiUrl;

  @Value("${sandbox.debias.connectTimeout}")
  private int connectTimeout;

  @Value("${sandbox.debias.requestTimeout}")
  private int requestTimeout;

  @Bean
  DeBiasStateService debiasMachine(DatasetDeBiasRepository datasetDeBiasRepository,
      DatasetRepository datasetRepository,
      RecordDeBiasMainRepository recordDeBiasMainRepository,
      RecordDeBiasDetailRepository recordDeBiasDetailRepository,
      ExecutionRecordRepository executionRecordRepository) {
    return new DeBiasStateService(datasetDeBiasRepository,
        datasetRepository,
        recordDeBiasMainRepository,
        recordDeBiasDetailRepository,
        executionRecordRepository);
  }

  @Bean
  DeBiasClient deBiasClient() {
    return new DeBiasClient(apiUrl, connectTimeout, requestTimeout);
  }
}

