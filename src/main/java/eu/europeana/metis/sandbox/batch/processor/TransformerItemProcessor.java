package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.config.BatchJobType.TRANSFORMATION;

import eu.europeana.metis.sandbox.batch.config.BatchJobType;
import eu.europeana.metis.sandbox.batch.config.ExecutionRecordUtil;
import eu.europeana.metis.sandbox.batch.config.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
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
public class TransformerItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final BatchJobType batchJobType = TRANSFORMATION;

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
  @Value("#{jobParameters['overrideJobId'] ?: stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  private final ItemProcessorUtil<String> itemProcessorUtil;

  public TransformerItemProcessor() {
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), Function.identity());
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
    return executionRecordDTO -> {
      final byte[] contentBytes = executionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8);
      final String resultString;
      InputStream xsltInputStream = new ByteArrayInputStream(xsltContent.getBytes(StandardCharsets.UTF_8));
      String datasetIdDatasetName = getJoinDatasetIdDatasetName(datasetId, datasetName);
      try (XsltTransformer xsltTransformer =
          new XsltTransformer("xsltKey", xsltInputStream, datasetIdDatasetName, datasetCountry, datasetLanguage);
          StringWriter writer = xsltTransformer.transform(contentBytes, prepareEuropeanaGeneratedIdsMap(contentBytes))) {
        resultString = writer.toString();
      }
      return resultString;
    };
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, jobInstanceId.toString());
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
