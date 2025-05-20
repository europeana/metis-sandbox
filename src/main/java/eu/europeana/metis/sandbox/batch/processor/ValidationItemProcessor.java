package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.VALIDATION;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchBatchJobSubType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import jakarta.annotation.PostConstruct;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Function;
import lombok.Setter;
import lombok.experimental.StandardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component("validationItemProcessor")
@StepScope
@Setter
public class ValidationItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final BatchJobType batchJobType = VALIDATION;

  @Value("#{jobParameters['batchJobSubType']}")
  private ValidationBatchBatchJobSubType batchJobSubType;
  @Value("#{jobParameters['overrideJobId'] ?: stepExecution.jobExecution.jobInstance.id}")
  private Long jobInstanceId;

  private static final String EDM_SORTER_FILE_URL = "http://ftp.eanadev.org/schema_zips/edm_sorter_20230809.xsl";
  private static final Properties properties = new Properties();
  private String schema;
  private String rootFileLocation;
  private String schematronFileLocation;
  private ValidationExecutionService validationService;
  private final ItemProcessorUtil<String> itemProcessorUtil;

  public ValidationItemProcessor() {
    prepareProperties();
    validationService = new ValidationExecutionService(properties);
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), Function.identity());
  }

  @PostConstruct
  private void postConstruct() {
    switch (batchJobSubType) {
      case EXTERNAL -> {
        schema = properties.getProperty("predefinedSchemas.edm-external.url");
        rootFileLocation = properties.getProperty("predefinedSchemas.edm-external.rootLocation");
        schematronFileLocation = properties.getProperty("predefinedSchemas.edm-external.schematronLocation");
      }
      case INTERNAL -> {
        schema = properties.getProperty("predefinedSchemas.edm-internal.url");
        rootFileLocation = properties.getProperty("predefinedSchemas.edm-internal.rootLocation");
        schematronFileLocation = properties.getProperty("predefinedSchemas.edm-internal.schematronLocation");
      }
      default -> throw new IllegalStateException("Unexpected value: " + batchJobSubType);
    }
  }

  private void prepareProperties() {
    properties.setProperty("predefinedSchemas", "localhost");

    properties.setProperty("predefinedSchemas.edm-internal.url",
        "http://ftp.eanadev.org/schema_zips/europeana_schemas-20241127.zip");
    properties.setProperty("predefinedSchemas.edm-internal.rootLocation", "EDM-INTERNAL.xsd");
    properties.setProperty("predefinedSchemas.edm-internal.schematronLocation", "schematron/schematron-internal.xsl");

    properties.setProperty("predefinedSchemas.edm-external.url",
        "http://ftp.eanadev.org/schema_zips/europeana_schemas-20241127.zip");
    properties.setProperty("predefinedSchemas.edm-external.rootLocation", "EDM.xsd");
    properties.setProperty("predefinedSchemas.edm-external.schematronLocation", "schematron/schematron.xsl");
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
    return executionRecord -> {
      final String reorderedFileContent;
      reorderedFileContent = reorderFileContent(executionRecord.getRecordData());

      ValidationResult result =
          validationService.singleValidation(schema, rootFileLocation, schematronFileLocation, reorderedFileContent);
      if (result.isSuccess()) {
        LOGGER.debug("Validation Success for datasetId {}, recordId {}", executionRecord.getDatasetId(),
            executionRecord.getRecordId());
      } else {
        LOGGER.info("Validation Failure for datasetId {}, recordId {}", executionRecord.getDatasetId(),
            executionRecord.getRecordId());
        throw new ValidationFailureException(result.getMessage());
      }
      return executionRecord.getRecordData();
    };
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
//    LOGGER.info("ValidationItemProcessor thread: {}", Thread.currentThread());
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, batchJobSubType,
        jobInstanceId.toString());
  }

  private String reorderFileContent(String recordData) throws TransformationException {
    LOGGER.debug("Reordering the file");
    try (XsltTransformer xsltTransformer = new XsltTransformer(EDM_SORTER_FILE_URL)) {
      StringWriter writer = xsltTransformer.transform(recordData.getBytes(StandardCharsets.UTF_8), null);
      return writer.toString();
    }
  }

  @StandardException
  private static class ValidationFailureException extends Exception {
  }
}
