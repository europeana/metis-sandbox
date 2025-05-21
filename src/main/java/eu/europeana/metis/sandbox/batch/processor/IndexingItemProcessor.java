package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.INDEXING;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.tiers.TierCalculationMode;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Date;
import java.util.function.Function;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component("indexingItemProcessor")
@StepScope
@Setter
public class IndexingItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final BatchJobType batchJobType = INDEXING;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  private final ItemProcessorUtil<String> itemProcessorUtil;
  private final Indexer indexer;
  private final IndexingProperties indexingProperties;

  public IndexingItemProcessor(Indexer indexer) {
    this.itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), Function.identity());
    this.indexer = indexer;
    this.indexingProperties = new IndexingProperties(new Date(), false, Collections.emptyList(), false, TierCalculationMode.OVERWRITE);
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
    return executionRecordDTO -> {
      LOGGER.info("Indexing: {}", executionRecordDTO.getRecordId());
      indexer.index(executionRecordDTO.getRecordData(), indexingProperties, tier -> true);
      LOGGER.info("Indexed: {}", executionRecordDTO.getRecordId());
      return executionRecordDTO.getRecordData();
    };
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, targetExecutionId);
  }
}
