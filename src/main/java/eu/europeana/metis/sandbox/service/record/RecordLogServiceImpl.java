package eu.europeana.metis.sandbox.service.record;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntityKey;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import org.springframework.stereotype.Service;

@Service
class RecordLogServiceImpl implements RecordLogService {

  private final RecordLogRepository repository;

  public RecordLogServiceImpl(RecordLogRepository repository) {
    this.repository = repository;
  }

  @Override
  public void logRecordEvent(Event recordEvent) {
    requireNonNull(recordEvent, "Record event must not be null");

    var record = recordEvent.getBody();
    var eventError = recordEvent.getEventError();
    String errorMessage = null;
    String stackTrace = null;

    if (eventError.isPresent()) {
      errorMessage = eventError.get().getMessage();
      stackTrace = eventError.get().getStackTrace();
    }

    var key = RecordLogEntityKey.builder()
        .id(record.getRecordId())
        .datasetId(record.getDatasetId())
        .step(recordEvent.getStep())
        .build();
    var recordLogEntity = new RecordLogEntity(key, record.getContentString(), recordEvent.getStatus(),
        errorMessage, stackTrace);

    try {
      repository.save(recordLogEntity);
    } catch (RuntimeException e) {
      throw new ServiceException("Error saving record event log: " + e.getMessage(), e);
    }
  }
}
