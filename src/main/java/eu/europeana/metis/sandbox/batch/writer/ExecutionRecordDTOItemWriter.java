package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExceptionLog;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class ExecutionRecordDTOItemWriter implements ItemWriter<ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  @Autowired
  public ExecutionRecordDTOItemWriter(ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  @Override
  public void write(Chunk<? extends ExecutionRecordDTO> chunk) {
    LOGGER.info("In writer writing chunk");
    final ArrayList<ExecutionRecord> executionRecords = new ArrayList<>();
    final ArrayList<ExecutionRecordExceptionLog> executionRecordExceptionLogs = new ArrayList<>();
    final ArrayList<ExecutionRecordTierContext> executionRecordTierContexts = new ArrayList<>();
    for (ExecutionRecordDTO executionRecordDTO : chunk) {
      if (StringUtils.isNotBlank(executionRecordDTO.getRecordData())) {
        executionRecords.add(ExecutionRecordUtil.converterToExecutionRecord(executionRecordDTO));
        Optional<ExecutionRecordTierContext> executionRecordTierContext =
            ExecutionRecordUtil.converterToExecutionRecordTierContext(executionRecordDTO);
        executionRecordTierContext.ifPresent(executionRecordTierContexts::add);
      } else {
        executionRecordExceptionLogs.add(ExecutionRecordUtil.converterToExecutionRecordExceptionLog(executionRecordDTO));
      }
    }
    LOGGER.info("In writer before saveAll");
    executionRecordRepository.saveAll(executionRecords);
    executionRecordTierContextRepository.saveAll(executionRecordTierContexts);
    executionRecordExceptionLogRepository.saveAll(executionRecordExceptionLogs);
    LOGGER.info("In writer finished writing chunk");
  }
}

