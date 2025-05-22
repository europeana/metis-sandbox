package eu.europeana.metis.sandbox.batch.processor;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import jakarta.annotation.PostConstruct;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
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
public class ValidationItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['batchJobSubType']}")
  private ValidationBatchBatchJobSubType batchJobSubType;

  private static final String EDM_SORTER_FILE_URL = "http://ftp.eanadev.org/schema_zips/edm_sorter_20230809.xsl";
  private static final Properties properties = new Properties();
  private String schema;
  private String rootFileLocation;
  private String schematronFileLocation;
  private ValidationExecutionService validationService;
  private final ItemProcessorUtil itemProcessorUtil;
  private final PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointRepository executionPointRepository;

  public ValidationItemProcessor(PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService,
      ExecutionPointRepository executionPointRepository) {
    prepareProperties();
    validationService = new ValidationExecutionService(properties);
    itemProcessorUtil = new ItemProcessorUtil(processSuccessRecord());
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointRepository = executionPointRepository;
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
  public ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> processSuccessRecord() {
    return successExecutionRecordDTO -> {
      final String reorderedFileContent;
      reorderedFileContent = reorderFileContent(successExecutionRecordDTO.getRecordData());

      ValidationResult result =
          validationService.singleValidation(schema, rootFileLocation, schematronFileLocation, reorderedFileContent);
      if (result.isSuccess()) {
        LOGGER.debug("Validation Success for datasetId {}, recordId {}", successExecutionRecordDTO.getDatasetId(),
            successExecutionRecordDTO.getRecordId());
      } else {
        LOGGER.info("Validation Failure for datasetId {}, recordId {}", successExecutionRecordDTO.getDatasetId(),
            successExecutionRecordDTO.getRecordId());
        throw new ValidationFailureException(result.getMessage());
      }

      return successExecutionRecordDTO.toBuilderOnlyIdentifiers(targetExecutionId, getExecutionName(batchJobSubType))
                                      .recordData(successExecutionRecordDTO.getRecordData()).build();
    };
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO successExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    ExecutionRecordDTO resultExecutionRecordDTO = itemProcessorUtil.processCapturingException(successExecutionRecordDTO,
        targetExecutionId, getExecutionName(batchJobSubType));
    if (batchJobSubType == ValidationBatchBatchJobSubType.INTERNAL) {
      generatePatternAnalysis(successExecutionRecordDTO);
    }
    return resultExecutionRecordDTO;
  }

  private void generatePatternAnalysis(SuccessExecutionRecordDTO successExecutionRecordDTO) {
    Optional<ExecutionPoint> executionPoint = executionPointRepository.findFirstByDatasetIdAndExecutionStepOrderByExecutionTimestampDesc(
        successExecutionRecordDTO.getDatasetId(), Step.VALIDATE_INTERNAL.name());
    if (executionPoint.isEmpty()) {
      throw new IllegalStateException("No execution point found for datasetId " + successExecutionRecordDTO.getDatasetId());
    }
    try {
      patternAnalysisService.generateRecordPatternAnalysis(executionPoint.get(), successExecutionRecordDTO.getRecordData());
    } catch (PatternAnalysisException e) {
      LOGGER.error(format("An error occurred while processing pattern analysis with record id %s",
          successExecutionRecordDTO.getRecordId()), e);
    }
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
