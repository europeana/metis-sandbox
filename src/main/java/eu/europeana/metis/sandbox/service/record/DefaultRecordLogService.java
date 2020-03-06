package eu.europeana.metis.sandbox.service.record;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntityKey;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class DefaultRecordLogService implements RecordLogService {

  private RecordLogRepository repository;

  public DefaultRecordLogService(RecordLogRepository repository) {
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
      //log.info("Saving record {}", record.getRecordId());
    } catch (Exception e) {
      throw new ServiceException("Error saving record log: " + e.getMessage(), e);
    }
  }
}
