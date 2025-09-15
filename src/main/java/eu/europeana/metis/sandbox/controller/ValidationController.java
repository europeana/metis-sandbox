package eu.europeana.metis.sandbox.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.sandbox.dto.validation.RecordValidationMessage;
import eu.europeana.metis.sandbox.dto.validation.RecordValidationMessage.Type;
import eu.europeana.metis.sandbox.dto.validation.ValidationResult;
import eu.europeana.metis.sandbox.dto.validation.ValidationResult.Status;
import eu.europeana.metis.sandbox.dto.validation.ValidationWorkflowReport;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller responsible for validating xml records.
 */
@RestController
@RequestMapping("/record/")
@Tag(name = "Record validation controller")
public class ValidationController {

  private final DatasetExecutionService datasetExecutionService;
  private final DatasetReportService datasetReportService;
  private final PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointService executionPointService;

  /**
   * Constructor.
   *
   * @param datasetExecutionService The service responsible for dataset execution processes.
   * @param datasetReportService The service responsible for generating dataset reports.
   * @param patternAnalysisService The service for analyzing patterns within datasets.
   * @param executionPointService The service managing execution points for validation processes.
   */
  public ValidationController(DatasetExecutionService datasetExecutionService,
      DatasetReportService datasetReportService, PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService,
      ExecutionPointService executionPointService) {
    this.datasetExecutionService = datasetExecutionService;
    this.datasetReportService = datasetReportService;
    this.patternAnalysisService = patternAnalysisService;
    this.executionPointService = executionPointService;
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
      @RequestParam("recordToValidate") MultipartFile recordToValidate) throws IOException {
    checkArgument(isFileTypeValid(recordToValidate), "It is expected for there to be one single xml record file");
    String datasetName = "direct_validation_" + UUID.randomUUID();
    DatasetMetadataRequest datasetMetadataRequest = DatasetMetadataRequest.builder()
                                                                          .datasetName(datasetName)
                                                                          .country(country)
                                                                          .language(language).build();
    String createdDatasetId = datasetExecutionService.createAndExecuteDatasetForFileValidationBlocking(
        datasetMetadataRequest, recordToValidate);
    ExecutionProgressInfoDTO executionProgressInfoDto = datasetReportService.getProgress(createdDatasetId);

    List<ValidationResult> validationResults = new ArrayList<>();
    for (ExecutionProgressByStepDTO executionProgressByStepDto : executionProgressInfoDto.executionProgressByStepDTOS()) {
      final ValidationResult validationResult;
      Optional<ErrorInfoDTO> errorInfoDto = executionProgressByStepDto.errors().stream().findFirst();
      validationResult = errorInfoDto.map(infoDto -> new ValidationResult(executionProgressByStepDto.step(),
                                         List.of(new RecordValidationMessage(Type.ERROR, infoDto.errorMessage())), Status.FAILED))
                                     .orElseGet(() -> new ValidationResult(executionProgressByStepDto.step(),
                                         List.of(new RecordValidationMessage(Type.INFO, "success")), Status.PASSED));
      validationResults.add(validationResult);
    }

    final Optional<ExecutionPoint> executionPointOptional = executionPointService
        .getExecutionPoint(createdDatasetId, FullBatchJobType.VALIDATE_INTERNAL.toString());
    Optional<? extends DatasetProblemPatternAnalysis<FullBatchJobType>> datasetPatternAnalysis =
        executionPointOptional.flatMap(executionPoint -> patternAnalysisService.getDatasetPatternAnalysis(
            createdDatasetId, FullBatchJobType.VALIDATE_INTERNAL, executionPoint.getExecutionTimestamp()));

    return new ValidationWorkflowReport(validationResults,
        datasetPatternAnalysis.map(DatasetProblemPatternAnalysis::getProblemPatternList).orElse(List.of()));
  }

  private boolean isFileTypeValid(MultipartFile fileToCheck) {
    String fileType = fileToCheck.getContentType();
    if (StringUtils.isBlank(fileType)) {
      throw new IllegalArgumentException("Something went wrong checking file's content type.");
    } else {
      return StringUtils.containsIgnoreCase(fileType, FileType.XML.name());

    }
  }
}
