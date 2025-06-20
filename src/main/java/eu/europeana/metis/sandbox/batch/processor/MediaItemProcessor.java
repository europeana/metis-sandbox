package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.service.workflow.MediaService;
import eu.europeana.metis.sandbox.service.workflow.MediaService.MediaProcessingResult;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for media extraction.
 */
@Slf4j
@StepScope
@Component("mediaItemProcessor")
public class MediaItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  private final MediaService mediaService;

  /**
   * Constructor with service parameter.
   *
   * @param mediaService The service responsible for media extraction record data.
   */
  public MediaItemProcessor(MediaService mediaService) {
    this.mediaService = mediaService;
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      log.debug("MediaItemProcessor thread: {}", Thread.currentThread());
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.successExecutionRecordDTO();
      MediaProcessingResult mediaProcessingResult = mediaService.processMediaRecord(
          originSuccessExecutionRecordDTO.getRecordData(),
          originSuccessExecutionRecordDTO.getDatasetId()
      );

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.targetExecutionId(),
          jobMetadataDTO.targetExecutionName(),
          b -> b.recordData(mediaProcessingResult.updatedRecordData())
                .exceptionWarnings(new HashSet<>(mediaProcessingResult.warningExceptions())));
    };
  }
}
