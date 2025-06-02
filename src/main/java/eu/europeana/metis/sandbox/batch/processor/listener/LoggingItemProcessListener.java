package eu.europeana.metis.sandbox.batch.processor.listener;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdAccess;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.HasExecutionRecordIdAccess;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class LoggingItemProcessListener<T extends HasExecutionRecordIdAccess<? extends ExecutionRecordIdAccess>>
    implements ItemProcessListener<T, Future<ExecutionRecordDTO>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void beforeProcess(@NotNull T item) {
    LOGGER.debug("beforeProcess");
  }

  @Override
  public void afterProcess(@NotNull T item, Future<ExecutionRecordDTO> future) {
    ExecutionRecordIdAccess executionRecordIdAccess = item.getIdentifier();

    final StringBuilder logBuilder = new StringBuilder(String.format(
        "Processing datasetId: %s, executionId: %s, executionName: %s, sourceRecordId: %s",
        executionRecordIdAccess.getDatasetId(),
        executionRecordIdAccess.getExecutionId(),
        executionRecordIdAccess.getExecutionName(),
        executionRecordIdAccess.getSourceRecordId()
    ));

    if (executionRecordIdAccess instanceof ExecutionRecordIdentifierKey executionRecordIdentifierKey) {
      logBuilder.append(String.format(", recordId: %s", executionRecordIdentifierKey.getRecordId()));
    }

    LOGGER.info(logBuilder.toString());


  }

  @Override
  public void onProcessError(@NotNull T executionRecord, @NotNull Exception e) {
    LOGGER.error(" onProcessError");
  }

}
