package eu.europeana.metis.sandbox.batch.processor.listener;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.entity.HasExecutionRecordIdentifier;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class LoggingItemProcessListener<T extends HasExecutionRecordIdentifier> implements
    ItemProcessListener<T, Future<ExecutionRecordDTO>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void beforeProcess(@NotNull T item) {
    LOGGER.debug("beforeProcess");
  }

  @Override
  public void afterProcess(@NotNull T item, Future<ExecutionRecordDTO> future) {
    ExecutionRecordIdentifier identifier = item.getIdentifier();
    LOGGER.info("Processing datasetId, executionId, recordId: {}, {}, {}", identifier.getDatasetId(), identifier.getExecutionId(), identifier.getRecordId());
  }

  @Override
  public void onProcessError(@NotNull T executionRecord, @NotNull Exception e) {
    LOGGER.error(" onProcessError");
  }

}
