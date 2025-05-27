package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.common.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component("transformationItemProcessor")
@StepScope
@Setter
public class TransformerItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['datasetName']}")
  private String datasetName;
  @Value("#{jobParameters['datasetCountry']}")
  private String datasetCountry;
  @Value("#{jobParameters['datasetLanguage']}")
  private String datasetLanguage;
  @Value("#{jobParameters['xsltContent']}")
  private String xsltContent;

  private final ItemProcessorUtil itemProcessorUtil;
  private ThrowingFunction<TransformationInput, String> transformationFunction;

  private record TransformationInput(String recordId, byte[] contentBytes, InputStream xsltInputStream) {

  }

  public TransformerItemProcessor() {
    itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @PostConstruct
  private void postConstruct() {
    transformationFunction = switch ((TransformationBatchJobSubType) getBatchJobSubType()) {
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
