package eu.europeana.metis.sandbox.service.record;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import java.nio.charset.StandardCharsets;
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
    requireNonNull(recordEvent, "Record event must not be null");

    var record = recordEvent.getBody();
    var recordErrors = recordEvent.getRecordErrors();

    var recordLogEntity = new RecordLogEntity(record.getRecordId(), record.getDatasetId(),
        recordEvent.getStep(), recordEvent.getStatus(), new String(record.getContent(), StandardCharsets.UTF_8));
    var recordErrorLogEntities = recordErrors.stream()
        .map(error -> new RecordErrorLogEntity(record.getRecordId(), record.getDatasetId(),
            recordEvent.getStep(), recordEvent.getStatus(), error.getMessage(),
            error.getStackTrace()))
        .collect(toList());

    try {
      recordLogRepository.save(recordLogEntity);
      errorLogRepository.saveAll(recordErrorLogEntities);
    } catch (RuntimeException e) {
      throw new ServiceException("Error saving record event log: " + e.getMessage(), e);
    }
  }
}
