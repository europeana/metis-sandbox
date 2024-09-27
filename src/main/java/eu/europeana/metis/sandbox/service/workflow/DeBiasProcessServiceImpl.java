package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

@Service
class DeBiasProcessServiceImpl implements DeBiasProcessService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeBiasProcessServiceImpl.class);

  private final LockRegistry lockRegistry;

  public DeBiasProcessServiceImpl(LockRegistry lockRegistry) {
    this.lockRegistry = lockRegistry;
  }

  @Override
  public List<RecordInfo> process(List<Record> recordToProcess) {
    Objects.requireNonNull(recordToProcess, "List of records is required");
    recordToProcess.forEach(record -> {

      LOGGER.info("DeBias Execution over: {}", record.getRecordId(), record.getContent());
    });

    return List.of();
  }
}
