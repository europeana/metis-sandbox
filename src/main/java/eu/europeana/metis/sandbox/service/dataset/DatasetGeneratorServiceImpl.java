package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class DatasetGeneratorServiceImpl implements DatasetGeneratorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasetGeneratorServiceImpl.class);

  private final RecordRepository recordRepository;

  public DatasetGeneratorServiceImpl(
      RecordRepository recordRepository) {
    this.recordRepository = recordRepository;
  }

  @Override
  public Dataset generate(DatasetMetadata datasetMetadata, List<ByteArrayInputStream> records) {
    validateDatasetMetadataAndRecords(datasetMetadata, records);

    final Set<Record> processedRecords = processRecordsAndRemoveDuplicates(datasetMetadata, records);

    return new Dataset(datasetMetadata.getDatasetId(), processedRecords, records.size() - processedRecords.size());
  }

  private void validateDatasetMetadataAndRecords(DatasetMetadata datasetMetadata, List<ByteArrayInputStream> records) {
    requireNonNull(datasetMetadata.getDatasetName(), "Dataset name must not be null");
    requireNonNull(datasetMetadata.getCountry(), "Country must not be null");
    requireNonNull(datasetMetadata.getLanguage(), "Language must not be null");
    requireNonNull(records, "Records must not be null");
    checkArgument(!records.isEmpty(), "Records must not be empty");
  }

  private Set<Record> processRecordsAndRemoveDuplicates(DatasetMetadata datasetMetadata, List<ByteArrayInputStream> records) {
    return records.stream()
            .map(ByteArrayInputStream::readAllBytes)
            .map(recordItem -> getOptionalRecordFromProcessorService(datasetMetadata, recordItem))
            .flatMap(Optional::stream)
            .collect(toSet());
  }


  private Optional<Record> getOptionalRecordFromProcessorService(DatasetMetadata datasetMetadata, byte[] recordContent) {
    try {
      final RecordEntity recordEntity = recordRepository.save(new RecordEntity(null, null, datasetMetadata.getDatasetId(), "", ""));
      final Long recordId = recordEntity.getId();
      return Optional.of(Record.builder()
                               .recordId(recordId)
                               .datasetId(datasetMetadata.getDatasetId())
                               .datasetName(datasetMetadata.getDatasetName())
                               .country(datasetMetadata.getCountry())
                               .language(datasetMetadata.getLanguage())
                               .content(recordContent)
                               .build());
    } catch (RuntimeException processorServiceException) {
      LOGGER.error("Failed to get record from processor service {} :: {} ", new String(recordContent), processorServiceException);
      return Optional.empty();
    }
  }
}
