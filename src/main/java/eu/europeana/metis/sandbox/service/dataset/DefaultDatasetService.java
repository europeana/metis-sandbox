package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class DefaultDatasetService implements DatasetService {

  private DatasetGeneratorService generatorService;
  private DatasetRepository datasetRepository;
  private AsyncDatasetPublishService publishService;

  public DefaultDatasetService(
      DatasetGeneratorService generatorService,
      DatasetRepository datasetRepository,
      AsyncDatasetPublishService publishService) {
    this.generatorService = generatorService;
    this.datasetRepository = datasetRepository;
    this.publishService = publishService;
  }

  @Override
  public String createDataset(String datasetName, Country country, Language language,
      List<String> records) {
    requireNonNull(datasetName, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");

    var entity = new DatasetEntity(datasetName, records.size());
    String id;
    try {
      id = datasetRepository.save(entity).getDatasetId();
    } catch (Exception e) {
      throw new ServiceException("Error creating the dataset: " + e.getMessage(), e);
    }

    var dataset = generatorService.generate(id, datasetName, country, language, records);
    publishService.publish(dataset);
    return dataset.getDatasetId();
  }
}
