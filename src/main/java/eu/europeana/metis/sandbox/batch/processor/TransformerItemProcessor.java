package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.common.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@StepScope
@Component("transformationItemProcessor")
public class TransformerItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

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

  private final TransformXsltRepository transformXsltRepository;
  private final ItemProcessorUtil itemProcessorUtil;
  private ThrowingFunction<TransformationInput, String> transformationFunction;

  private record TransformationInput(String recordId, byte[] contentBytes, InputStream xsltInputStream) {

  }

  public TransformerItemProcessor(TransformXsltRepository transformXsltRepository) {
    this.transformXsltRepository = transformXsltRepository;
    itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @PostConstruct
  private void postConstruct() {
    transformationFunction = switch ((TransformationBatchJobSubType) getFullBatchJobType().getBatchJobSubType()) {
      case EXTERNAL -> (transformationInput) -> {
        try (XsltTransformer xsltTransformer =
            new XsltTransformer(transformationInput.recordId, transformationInput.xsltInputStream);
            StringWriter writer = xsltTransformer.transform(transformationInput.contentBytes, null)) {
          return writer.toString();
        }
      };

      case INTERNAL -> (transformationInput) -> {
        final String datasetIdDatasetName = getJoinDatasetIdDatasetName(datasetId, datasetName);
        final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = prepareEuropeanaGeneratedIdsMap(
            transformationInput.contentBytes);
        try (XsltTransformer xsltTransformer =
            new XsltTransformer("xsltKey", transformationInput.xsltInputStream, datasetIdDatasetName, datasetCountry,
                datasetLanguage);
            StringWriter writer = xsltTransformer.transform(transformationInput.contentBytes, europeanaGeneratedIdsMap)) {
          return writer.toString();
        }
      };
    };
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    JobMetadataDTO jobMetadataDTO = new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(),
        getTargetExecutionId());
    return itemProcessorUtil.processCapturingException(jobMetadataDTO);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      final byte[] contentBytes = originSuccessExecutionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8);
      String xsltContent = transformXsltRepository.findById(Integer.valueOf(xsltId))
                                                  .map(TransformXsltEntity::getTransformXslt).orElseThrow();
      InputStream xsltInputStream = new ByteArrayInputStream(xsltContent.getBytes(StandardCharsets.UTF_8));

      final String resultString = transformationFunction.apply(
          new TransformationInput(originSuccessExecutionRecordDTO.getRecordId(), contentBytes, xsltInputStream));

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(resultString));
    };
  }

  private EuropeanaGeneratedIdsMap prepareEuropeanaGeneratedIdsMap(byte[] content)
      throws EuropeanaIdException {
    EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
    if (!StringUtils.isBlank(datasetId)) {
      String fileDataString = new String(content, StandardCharsets.UTF_8);
      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(fileDataString, datasetId);
    }
    return europeanaGeneratedIdsMap;
  }

  private String getJoinDatasetIdDatasetName(String datasetId, String datasetName) {
    return String.join("_", datasetId, datasetName);
  }
}
