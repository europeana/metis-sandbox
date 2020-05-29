package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DatasetServiceImpl implements DatasetService {

  private final DatasetGeneratorService generatorService;
  private final DatasetRepository datasetRepository;
  private final AsyncDatasetPublishService publishService;

  public DatasetServiceImpl(
      DatasetGeneratorService generatorService,
      DatasetRepository datasetRepository,
      AsyncDatasetPublishService publishService) {
    this.generatorService = generatorService;
    this.datasetRepository = datasetRepository;
    this.publishService = publishService;
  }

  @Transactional
  @Override
  public Dataset createDataset(String datasetName, Country country, Language language,
      List<ByteArrayInputStream> records) {
    requireNonNull(datasetName, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");

    var entity = new DatasetEntity(datasetName, records.size());
    try {
      entity = datasetRepository.save(entity);
    } catch (Exception e) {
      throw new ServiceException("Error creating the dataset: " + e.getMessage(), e);
    }

    var dataset = generatorService
        .generate(entity.getDatasetId(), datasetName, country, language, records);

    // if there are duplicate records in the original list
    if (dataset.getRecords().size() < records.size()) {
      // adjust records qty to be equal to non duplicate records
      entity.setRecordsQuantity(dataset.getRecords().size());
      try {
        datasetRepository.save(entity);
      } catch (Exception e) {
        throw new ServiceException("Error updating the dataset: " + e.getMessage(), e);
      }
    }

    publishService.publish(dataset);
    return dataset;
  }
}
