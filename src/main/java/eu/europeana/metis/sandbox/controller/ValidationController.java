package eu.europeana.metis.sandbox.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.dataset.HarvestingParameterService;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.sandbox.service.validationworkflow.RecordValidationMessage;
import eu.europeana.metis.sandbox.service.validationworkflow.RecordValidationMessage.Type;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationResult;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationResult.Status;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationWorkflowReport;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationWorkflowService;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The type Validation controller.
 */
@RestController
@RequestMapping("/record/")
@Tag(name = "Record validation controller")
public class ValidationController {

  private static final String DATASET_ID_PREFIX = "direct_validation_";
  private final ValidationWorkflowService workflowService;
  private final DatasetService datasetService;
  private final DatasetReportService datasetReportService;
  private final BatchJobExecutor batchJobExecutor;
  private final PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointService executionPointService;
  private final HarvestingParameterService harvestingParameterService;

  /**
   * Instantiates a new Validation controller.
   *
   * @param validationWorkflowService the validation workflow service
   */
  public ValidationController(ValidationWorkflowService validationWorkflowService, DatasetService datasetService,
      DatasetReportService datasetReportService,
      BatchJobExecutor batchJobExecutor, PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService,
      ExecutionPointService executionPointService, HarvestingParameterService harvestingParameterService) {
    this.workflowService = validationWorkflowService;
    this.datasetService = datasetService;
    this.datasetReportService = datasetReportService;
    this.batchJobExecutor = batchJobExecutor;
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointService = executionPointService;
    this.harvestingParameterService = harvestingParameterService;
  }

  /**
   * Validate validation workflow report.
   *
   * @param country the country
   * @param language the language
   * @param recordToValidate the record to validate
   * @return the validation workflow report
   * @throws SerializationException the serialization exception
   * @throws IOException the io exception
   */
  @Operation(summary = "Validate a record in rdf+xml format", description = "Validation & field warnings")
  @ApiResponse(responseCode = "200", description = "Success")
  @ApiResponse(responseCode = "400", description = "Error")
  @PostMapping(value = "/validation", produces = APPLICATION_JSON_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
  @RequestBody(content = {@Content(mediaType = MULTIPART_FORM_DATA_VALUE)})
  @ResponseStatus(HttpStatus.OK)
  public ValidationWorkflowReport validate(
      @Parameter(description = "country of the record")
      @RequestParam(value = "country", required = false, defaultValue = "Europe") Country country,
      @Parameter(description = "language of the record")
      @RequestParam(value = "language", required = false, defaultValue = "Multilingual Content") Language language,
      @Parameter(description = "record file to be validated", required = true)
      @RequestParam("recordToValidate") MultipartFile recordToValidate) throws SerializationException, IOException {
    checkArgument(isFileTypeValid(recordToValidate), "It is expected for there to be one single xml record file");

    final String datasetName = DATASET_ID_PREFIX + UUID.randomUUID();
    final String createdDatasetId = datasetService.createEmptyDataset(WorkflowType.FILE_HARVEST_ONLY_VALIDATION, datasetName,
        null,
        country, language, null);
    final Path filePath;
    try {
      filePath = Files.createTempFile("dataset-" + createdDatasetId,
          "-" + recordToValidate.getOriginalFilename());
      Files.copy(recordToValidate.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ServiceException("Error harvesting records from file " + recordToValidate.getOriginalFilename(), e);
    }

    DatasetMetadata datasetMetadata = DatasetMetadata.builder().withDatasetId(createdDatasetId)
                                                     .withDatasetName(datasetName).withCountry(country).withLanguage(language)
                                                     .withStepSize(1).build();
    harvestingParameterService.createDatasetHarvestingParameters(datasetMetadata.getDatasetId(),
        new FileHarvestingDto(recordToValidate.getOriginalFilename(), "xml"));
    batchJobExecutor.executeBlocking(datasetMetadata, filePath);
    ProgressInfoDto progressInfoDto = datasetReportService.getProgress(datasetMetadata.getDatasetId());

    List<ValidationResult> validationResults = new ArrayList<>();
    for (ProgressByStepDto progressByStepDto : progressInfoDto.getProgressByStep()) {
      final ValidationResult validationResult;
      Optional<ErrorInfoDto> errorInfoDto = progressByStepDto.getErrors().stream().findFirst();
      validationResult = errorInfoDto.map(infoDto -> new ValidationResult(progressByStepDto.getStep(),
                                         new RecordValidationMessage(Type.ERROR, infoDto.getErrorMessage()), Status.FAILED))
                                     .orElseGet(() -> new ValidationResult(progressByStepDto.getStep(),
                                         new RecordValidationMessage(Type.INFO, "success"), Status.PASSED));
      validationResults.add(validationResult);
    }

    final Optional<ExecutionPoint> executionPointOptional = executionPointService
        .getExecutionPoint(datasetMetadata.getDatasetId(), Step.VALIDATE_INTERNAL.toString());
    Optional<DatasetProblemPatternAnalysis<Step>> datasetPatternAnalysis =
        executionPointOptional.flatMap(executionPoint -> patternAnalysisService.getDatasetPatternAnalysis(
            datasetMetadata.getDatasetId(), Step.VALIDATE_INTERNAL, executionPoint.getExecutionTimestamp()));

    return new ValidationWorkflowReport(validationResults,
        datasetPatternAnalysis.map(DatasetProblemPatternAnalysis::getProblemPatternList).orElse(List.of()));
  }

  private boolean isFileTypeValid(MultipartFile fileToCheck) {
    String fileType = fileToCheck.getContentType();
    if (fileType == null) {
      throw new IllegalArgumentException("Something went wrong checking file's content type.");
    } else {
      return fileType.contains("xml");
    }

  }
}
