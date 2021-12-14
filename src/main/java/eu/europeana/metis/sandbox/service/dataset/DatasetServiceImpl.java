package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.projection.DatasetIdView;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
      List<ByteArrayInputStream> records, boolean hasReachedRecordLimit) {
    requireNonNull(datasetName, "Dataset name must not be null");
    requireNonNull(country, "Country must not be null");
    requireNonNull(language, "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");

    var entity = new DatasetEntity(datasetName, records.size(), language, country, hasReachedRecordLimit);
    try {
      entity = datasetRepository.save(entity);
    } catch (Exception e) {
      throw new ServiceException(format("Error creating dataset: [%s]. ", datasetName), e);
    }

    var datasetId = String.valueOf(entity.getDatasetId());
    var dataset = generatorService
        .generate(datasetId, datasetName, country, language, records);

    // if there are duplicate records in the original list
    if (dataset.getRecords().size() < records.size()) {
      // adjust records qty to be equal to non duplicate records
      entity.setRecordsQuantity(dataset.getRecords().size());
      try {
        datasetRepository.save(entity);
      } catch (Exception e) {
        throw new ServiceException(format("Error updating dataset: [%s]. ", datasetName), e);
      }
    }

    publishService.publish(dataset);
    return dataset;
  }

  @Override
  public List<String> getDatasetIdsCreatedBefore(int days) {
    LocalDateTime date = LocalDateTime.now()
        .truncatedTo(ChronoUnit.DAYS)
        .minusDays(days);

    try {
      return datasetRepository.getByCreatedDateBefore(date).stream()
          .map(DatasetIdView::getDatasetId)
          .map(Object::toString)
          .collect(toList());
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error getting datasets older than %s days. ", days), e);
    }
  }

  @Override
  @Transactional
  public void remove(String datasetId) {
    try {
      datasetRepository.deleteById(Integer.valueOf(datasetId));
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error removing dataset id: [%s]. ", datasetId), e);
    }
  }
}
