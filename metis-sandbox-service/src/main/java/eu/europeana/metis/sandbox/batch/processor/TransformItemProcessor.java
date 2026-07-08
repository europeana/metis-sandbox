package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.common.batch.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.service.workflow.TransformService;
import eu.europeana.metis.sandbox.service.workflow.TransformService.TransformDatasetContext;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for transformation.
 */
@StepScope
@Component("transformItemProcessor")
public class TransformItemProcessor extends AbstractExecutionRecordMetisItemProcessor {

  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['datasetName']}")
  private String datasetName;
  @Value("#{jobParameters['datasetCountry']}")
  private String datasetCountry;
  @Value("#{jobParameters['datasetLanguage']}")
  private String datasetLanguage;
  @Value("#{jobParameters['xsltId']}")
  private String xsltId;

  private final TransformService transformService;
  private TransformDatasetContext transformDatasetContext;
  private byte[] xsltBytes;
  private String xsltCacheKey;

  /**
   * Constructor with service parameter.
   *
   * @param transformService The service responsible for transforming record data.
   */
  public TransformItemProcessor(TransformService transformService) {
    this.transformService = transformService;
  }

  /**
   * Prepares the processing context by loading the XSLT bytes and computing their cache key.
   * <p>
   * This method is executed before a processing step starts. It retrieves the XSLT bytes associated with the provided XSLT
   * identifier (`xsltId`) using the {@code TransformService}. The retrieved bytes are then used to compute an SHA-256 hash, which
   * serves as a cache key for transformation operations.
   */
  @PostConstruct
  private void beforeStep() {
    this.xsltBytes = transformService.getXsltBytes(xsltId);
    this.xsltCacheKey = "xslt-" + DigestUtils.sha256Hex(xsltBytes);
    this.transformDatasetContext = new TransformDatasetContext(datasetId, datasetName, datasetCountry, datasetLanguage);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();

      byte[] recordBytes = originSuccessExecutionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8);
      final String resultString = switch (getFullBatchJobType().requireBatchJobSubType(TransformationBatchJobSubType.class)) {
        case EXTERNAL -> transformService.transformExternal(recordBytes, xsltBytes, xsltCacheKey);
        case INTERNAL -> transformService.transformInternal(recordBytes, xsltBytes, xsltCacheKey, transformDatasetContext);
      };

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(resultString));
    };
  }
}
