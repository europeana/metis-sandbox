package eu.europeana.metis.sandbox.service.record;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RecordLogServiceImpl implements RecordLogService {

  private final RecordLogRepository recordLogRepository;
  private final RecordErrorLogRepository errorLogRepository;

  public RecordLogServiceImpl(RecordLogRepository recordLogRepository,
      RecordErrorLogRepository errorLogRepository) {
    this.recordLogRepository = recordLogRepository;
    this.errorLogRepository = errorLogRepository;
  }

  @Override
  @Transactional
  public void logRecordEvent(Event recordEvent) {
    var record = recordEvent.getBody();
    var recordErrors = recordEvent.getRecordErrors();

    var recordLogEntity = new RecordLogEntity(record.getRecordId(), record.getEuropeanaId(), record.getDatasetId(),
        recordEvent.getStep(), recordEvent.getStatus(), new String(record.getContent(), StandardCharsets.UTF_8));
    var recordErrorLogEntities = recordErrors.stream()
        .map(error -> new RecordErrorLogEntity(record.getRecordId(), record.getEuropeanaId(), record.getDatasetId(),
            recordEvent.getStep(), recordEvent.getStatus(), error.getMessage(),
            error.getStackTrace()))
        .collect(toList());

    try {
      recordLogRepository.save(recordLogEntity);
      errorLogRepository.saveAll(recordErrorLogEntities);
    } catch (RuntimeException e) {
      throw new ServiceException(
          format("Error saving record log for record: [%s]. ", record.getRecordId()), e);
    }
  }

  @Override
  public String getProviderRecordString(RecordIdType recordIdType, String recordId, String datasetId) {
    return Optional.ofNullable(getRecordLogEntity(recordIdType, recordId, datasetId)).map(RecordLogEntity::getContent)
        .orElse(StringUtils.EMPTY);
  }

  @Override
  public RecordLogEntity getRecordLogEntity(RecordIdType recordIdType, String recordId, String datasetId) {
    final RecordLogEntity recordLogEntity;
    if (recordIdType == RecordIdType.EUROPEANA_ID) {
      recordLogEntity = recordLogRepository.findRecordLogByEuropeanaIdAndDatasetIdAndStep(
          recordId, datasetId, Step.MEDIA_PROCESS);
    } else {
      recordLogEntity = recordLogRepository.findRecordLogByRecordIdAndDatasetIdAndStep(recordId, datasetId,
          Step.MEDIA_PROCESS);
    }
    return recordLogEntity;
  }

  @Override
  @Transactional
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");
    try {
      errorLogRepository.deleteByDatasetId(datasetId);
      recordLogRepository.deleteByDatasetId(datasetId);
    } catch (RuntimeException e) {
      throw new ServiceException(
          format("Error removing records for dataset id: [%s]. ", datasetId), e);
    }
  }
}
