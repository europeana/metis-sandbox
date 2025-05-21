package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.INDEXING;
import static java.lang.String.format;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.tiers.TierCalculationMode;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
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
    this.indexingProperties = new IndexingProperties(new Date(), false, Collections.emptyList(), false,
        TierCalculationMode.OVERWRITE);
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
    return executionRecordDTO -> {
      LOGGER.info("Indexing: {}", executionRecordDTO.getRecordId());

      InputStream inputStream = new ByteArrayInputStream(executionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8));
      TierResults tierResults = indexer.indexAndGetTierCalculations(inputStream, indexingProperties);

      if (tierResults == null || isAllDataNull(tierResults)) {
        throw new IndexerRelatedIndexingException(
            format("Something went wrong with tier calculations with record %s", executionRecordDTO.getRecordId()));
      }

      setTierResults(executionRecordDTO, tierResults);
      LOGGER.info("Indexed: {}", executionRecordDTO.getRecordId());
      return executionRecordDTO.getRecordData();
    };
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, targetExecutionId);
  }

  public void setTierResults(ExecutionRecordDTO executionRecordDTO, TierResults tierResults) {
    executionRecordDTO.setContentTier(tierResults.getMediaTier().toString());
    executionRecordDTO.setMetadataTier(tierResults.getMetadataTier().toString());
    executionRecordDTO.setContentTierBeforeLicenseCorrection(tierResults.getContentTierBeforeLicenseCorrection().toString());
    executionRecordDTO.setMetadataTierLanguage(tierResults.getMetadataTierLanguage().toString());
    executionRecordDTO.setMetadataTierEnablingElements(tierResults.getMetadataTierEnablingElements().toString());
    executionRecordDTO.setMetadataTierContextualClasses(tierResults.getMetadataTierContextualClasses().toString());
    executionRecordDTO.setLicense(tierResults.getLicenseType().toString());
  }

  private boolean isAllDataNull(TierResults tierResultsToCheck) {
    return tierResultsToCheck.getMediaTier() == null &&
        tierResultsToCheck.getMetadataTier() == null &&
        tierResultsToCheck.getContentTierBeforeLicenseCorrection() == null &&
        tierResultsToCheck.getMetadataTierLanguage() == null &&
        tierResultsToCheck.getMetadataTierContextualClasses() == null &&
        tierResultsToCheck.getMetadataTierEnablingElements() == null &&
        tierResultsToCheck.getLicenseType() == null;
  }
}
