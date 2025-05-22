package eu.europeana.metis.sandbox.batch.processor;

import static java.lang.String.format;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.tiers.TierCalculationMode;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
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
public class IndexingItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  private final ItemProcessorUtil itemProcessorUtil;
  private final Indexer indexer;
  private final IndexingProperties indexingProperties;

  public IndexingItemProcessor(Indexer indexer) {
    this.itemProcessorUtil = new ItemProcessorUtil(processSuccessRecord());
    this.indexer = indexer;
    this.indexingProperties = new IndexingProperties(new Date(), false, Collections.emptyList(), false,
        TierCalculationMode.OVERWRITE);
  }

  @Override
  public ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> processSuccessRecord() {
    return successExecutionRecordDTO -> {
      LOGGER.info("Indexing: {}", successExecutionRecordDTO.getRecordId());

      InputStream inputStream = new ByteArrayInputStream(
          successExecutionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8));
      TierResults tierResults = indexer.indexAndGetTierCalculations(inputStream, indexingProperties);

      if (tierResults == null || isAllDataNull(tierResults)) {
        throw new IndexerRelatedIndexingException(
            format("Something went wrong with tier calculations with record %s", successExecutionRecordDTO.getRecordId()));
      }

      LOGGER.info("Indexed: {}", successExecutionRecordDTO.getRecordId());
      return successExecutionRecordDTO.toBuilderOnlyIdentifiers(targetExecutionId, getExecutionName())
                                      .recordData(successExecutionRecordDTO.getRecordData())
                                      .tierResults(tierResults)
                                      .build();
    };
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO successExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    return itemProcessorUtil.processCapturingException(successExecutionRecordDTO, targetExecutionId, getExecutionName());
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
