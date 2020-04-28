package eu.europeana.metis.sandbox.service.record;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordErrorEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import org.springframework.stereotype.Service;

@Service
class RecordStoreServiceImpl implements RecordStoreService {

  private final RecordRepository repository;

  public RecordStoreServiceImpl(RecordRepository repository) {
    this.repository = repository;
  }

  @Override
  public void storeRecordEvent(Event recordEvent) {
    requireNonNull(recordEvent, "Record event must not be null");

    var record = recordEvent.getBody();
    var eventError = recordEvent.getRecordErrors();

    var entity = new RecordEntity(record.getRecordId(), record.getDatasetId(),
        recordEvent.getStep(), recordEvent.getStatus(), record.getContentString());
    var errors = eventError.stream()
        .map(error -> new RecordErrorEntity(entity, error.getMessage(), error.getStackTrace()))
        .collect(toList());
    entity.setRecordErrors(errors);

    try {
      repository.save(entity);
    } catch (RuntimeException e) {
      throw new ServiceException("Error saving record event log: " + e.getMessage(), e);
    }
  }
}
