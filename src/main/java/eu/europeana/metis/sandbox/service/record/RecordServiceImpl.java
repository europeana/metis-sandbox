package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.dto.RecordTiersInfoDto;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to modify Records in database
 */
@Service
public class RecordServiceImpl implements RecordService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final RecordRepository recordRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  /**
   * Constructs an instance of RecordServiceImpl with the required dependencies.
   *
   * @param recordRepository the repository for managing Record entities
   * @param recordJdbcRepository the JDBC repository for Record-related operations
   * @param xmlRecordProcessorService the service for processing XML-based records
   */
  public RecordServiceImpl(RecordRepository recordRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.recordRepository = recordRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  @Override
  public List<RecordTiersInfoDto> getRecordsTiers(String datasetId) {
    List<ExecutionRecordTierContext> executionRecordTierContext = executionRecordTierContextRepository.findByIdentifier_DatasetId(datasetId);

    if (executionRecordTierContext.isEmpty()) {
      throw new InvalidDatasetException(datasetId);
    }

    return executionRecordTierContext.stream()
                         .filter(this::areAllTierValuesNotNullOrEmpty)
                         .map(RecordTiersInfoDto::new)
                         .toList();
  }

  @Override
  @Transactional
  public void remove(String datasetId) {
    recordRepository.deleteByDatasetId(datasetId);
  }

  private boolean areAllTierValuesNotNullOrEmpty(ExecutionRecordTierContext executionRecordTierContext) {
    return isContentTierValid(executionRecordTierContext) &&
        isMetadataTierValid(executionRecordTierContext);
  }

  private boolean isContentTierValid(ExecutionRecordTierContext executionRecordTierContext) {
    return StringUtils.isNotBlank(executionRecordTierContext.getContentTier()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getContentTierBeforeLicenseCorrection());
  }

  private boolean isMetadataTierValid(ExecutionRecordTierContext executionRecordTierContext) {
    return StringUtils.isNotBlank(executionRecordTierContext.getMetadataTier()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getMetadataTierLanguage()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getMetadataTierEnablingElements()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getMetadataTierContextualClasses()) &&
        StringUtils.isNotBlank(executionRecordTierContext.getLicense());
  }
}
