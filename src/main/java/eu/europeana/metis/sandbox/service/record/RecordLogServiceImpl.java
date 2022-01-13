package eu.europeana.metis.sandbox.service.record;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RecordLogServiceImpl implements RecordLogService {

  private final RecordLogRepository recordLogRepository;
  private final RecordErrorLogRepository errorLogRepository;
  private final RecordRepository recordRepository;

  public RecordLogServiceImpl(RecordLogRepository recordLogRepository,
      RecordErrorLogRepository errorLogRepository,
      RecordRepository recordRepository) {
    this.recordLogRepository = recordLogRepository;
    this.errorLogRepository = errorLogRepository;
    this.recordRepository = recordRepository;
  }

  @Override
  @Transactional
  public void logRecordEvent(Event recordEvent) {
    var record = recordEvent.getBody();
    var recordErrors = recordEvent.getRecordErrors();

    var recordLogEntity = new RecordLogEntity(new RecordEntity(record), recordEvent.getStep(), recordEvent.getStatus());
    var recordErrorLogEntities = recordErrors.stream()
        .map(error -> new RecordErrorLogEntity(new RecordEntity(record), recordEvent.getStep(),
            recordEvent.getStatus(), error.getMessage(),
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
  @Transactional
  public void remove(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");
    try {
      errorLogRepository.deleteByDatasetId(datasetId);
      recordLogRepository.deleteByDatasetId(datasetId);
      recordRepository.deleteByDatasetId(datasetId);
    } catch (RuntimeException e) {
      throw new ServiceException(
          format("Error removing records for dataset id: [%s]. ", datasetId), e);
    }
  }
}
