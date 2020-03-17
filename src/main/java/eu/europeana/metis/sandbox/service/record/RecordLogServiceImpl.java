package eu.europeana.metis.sandbox.service.record;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntityKey;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class RecordLogServiceImpl implements RecordLogService {

  private static final Logger log = LoggerFactory
      .getLogger(RecordLogServiceImpl.class);

  private RecordLogRepository repository;

  public RecordLogServiceImpl(RecordLogRepository repository) {
    this.repository = repository;
  }

  @Override
  public void logRecord(Record record) {
    requireNonNull(record, "Record must not be null");

    var key = RecordLogEntityKey.builder()
        .id(record.getRecordId())
        .datasetId(record.getDatasetId())
        .step(record.getStep())
        .build();
    var recordLogEntity = new RecordLogEntity(key, record.getContent(), record.getStatus());

    try {
      repository.save(recordLogEntity);
      log.info("Saving record {}", record.getRecordId());
    } catch (Exception e) {
      throw new ServiceException("Error saving record log: " + e.getMessage(), e);
    }
  }
}
