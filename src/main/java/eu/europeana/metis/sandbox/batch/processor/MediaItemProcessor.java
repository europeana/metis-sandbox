package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.service.workflow.MediaService;
import eu.europeana.metis.sandbox.service.workflow.MediaService.MediaProcessingResult;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@StepScope
@Component("mediaItemProcessor")
public class MediaItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final MediaService mediaService;
  private final ItemProcessorUtil itemProcessorUtil;

  public MediaItemProcessor(MediaService mediaService) {
    this.mediaService = mediaService;
    this.itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    JobMetadataDTO jobMetadataDTO = new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(),
        getTargetExecutionId());
    return itemProcessorUtil.processCapturingException(jobMetadataDTO);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      LOGGER.debug("MediaItemProcessor thread: {}", Thread.currentThread());
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      MediaProcessingResult mediaProcessingResult = mediaService.processMediaRecord(
          originSuccessExecutionRecordDTO.getRecordData(),
          originSuccessExecutionRecordDTO.getDatasetId()
      );

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(mediaProcessingResult.updatedRecordData())
                .exceptionWarnings(new HashSet<>(mediaProcessingResult.warningExceptions())));
    };
  }
}
