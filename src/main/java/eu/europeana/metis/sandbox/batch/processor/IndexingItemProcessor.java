package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;
import static java.lang.String.format;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.exception.IndexerRelatedIndexingException;
import eu.europeana.indexing.tiers.TierCalculationMode;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
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
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@StepScope
@Component("indexingItemProcessor")
public class IndexingItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ItemProcessorUtil itemProcessorUtil;
  private final Indexer indexer;
  private final IndexingProperties indexingProperties;

  public IndexingItemProcessor(Indexer indexer) {
    this.itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
    this.indexer = indexer;
    this.indexingProperties = new IndexingProperties(new Date(), false, Collections.emptyList(), false,
        TierCalculationMode.OVERWRITE);
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    JobMetadataDTO jobMetadataDTO = new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(), getTargetExecutionId());
    return itemProcessorUtil.processCapturingException(jobMetadataDTO);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      LOGGER.info("Indexing: {}", originSuccessExecutionRecordDTO.getRecordId());

      InputStream inputStream = new ByteArrayInputStream(
          originSuccessExecutionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8));
      TierResults tierResults = indexer.indexAndGetTierCalculations(inputStream, indexingProperties);

      if (tierResults == null || isAllDataNull(tierResults)) {
        throw new IndexerRelatedIndexingException(
            format("Something went wrong with tier calculations with record %s", originSuccessExecutionRecordDTO.getRecordId()));
      }

      LOGGER.info("Indexed: {}", originSuccessExecutionRecordDTO.getRecordId());
      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(originSuccessExecutionRecordDTO.getRecordData())
                .tierResults(tierResults));
    };
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
