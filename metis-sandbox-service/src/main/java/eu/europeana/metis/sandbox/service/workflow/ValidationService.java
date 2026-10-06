package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Optional.ofNullable;

import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.sandbox.common.batch.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

/**
 * Service class that provides methods to validate records and perform pattern analysis.
 */
@Slf4j
@AllArgsConstructor
@Service
public class ValidationService {

  private final ValidationExecutionService validationExecutionService;
  private final ObjectFactory<XsltTransformer> xsltTransformerFactory;
  private final PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;
  private final ExecutionPointRepository executionPointRepository;

  /**
   * Validates a single record based on the provided data and subtype and optionally performs pattern analysis for internal
   * records.
   *
   * <p>The record data may be reordered based on the subtype before validation.
   * <p>Pattern analysis is executed for internal records after validation.
   *
   * @param recordData the raw data of the record to be validated
   * @param recordId the unique identifier of the record being validated
   * @param datasetId the identifier of the dataset the record belongs to
   * @param executionPointId the identifier of the execution point associated with the record
   * @param subtype the subtype of the validation job (e.g., EXTERNAL or INTERNAL)
   * @return the result of the validation containing validation status and messages
   * @throws ValidationException if the validation process encounters an error
   */
  public ValidationResultWithIdentifiers validateRecord(String recordData, String recordId, String datasetId,
      Integer executionPointId, ValidationBatchJobSubType subtype) throws ValidationException {

    String reorderedFileContent = recordData;
    if (subtype == ValidationBatchJobSubType.EXTERNAL) {
      reorderedFileContent = reorderFileContent(recordData);
    }

    final String schema = resolveSchema(subtype);
    ValidationResult validationResult = validationExecutionService.singleValidation(schema, null, null, reorderedFileContent);
    if (!validationResult.isSuccess()) {
      log.warn("Validation Failure for datasetId {}, recordId {}", datasetId, recordId);
      throw new ValidationException(validationResult.getMessage());
    }

    Optional<EuropeanaGeneratedIdsMap> europeanaGeneratedIdsMap =
        handleSubtype(subtype, executionPointId, datasetId, recordData, reorderedFileContent);

    log.debug("Validation Success for datasetId {}, recordId {}", datasetId, recordId);
    return new ValidationResultWithIdentifiers(validationResult, europeanaGeneratedIdsMap);
  }

  private static @NotNull String resolveSchema(ValidationBatchJobSubType subtype) {
    return switch (subtype) {
      case EXTERNAL -> "EDM-EXTERNAL";
      case INTERNAL -> "EDM-INTERNAL";
    };
  }

  private String reorderFileContent(String recordData) throws ValidationException {
    try (XsltTransformer xsltTransformer = xsltTransformerFactory.getObject()) {
      return xsltTransformer
          .transform(recordData.getBytes(StandardCharsets.UTF_8), null)
          .toString();
    } catch (TransformationException e) {
      throw new ValidationException(e);
    }
  }

  private Optional<EuropeanaGeneratedIdsMap> handleSubtype(
      ValidationBatchJobSubType subtype,
      Integer executionPointId, String datasetId,
      String recordData,
      String reorderedFileContent
  ) {
    return switch (subtype) {
      case EXTERNAL -> getEuropeanaGeneratedIdsMap(datasetId, recordData);
      case INTERNAL -> {
        generatePatternAnalysis(executionPointId, reorderedFileContent);
        yield Optional.empty();
      }
    };
  }


  private void generatePatternAnalysis(Integer executionPointId, String reorderedRecordData) {
    ExecutionPoint executionPoint = executionPointRepository
        .findById(executionPointId)
        .orElseThrow(() -> new IllegalStateException("No execution point found for id " + executionPointId));
    try {
      patternAnalysisService.generateRecordPatternAnalysis(executionPoint, reorderedRecordData);
    } catch (PatternAnalysisException e) {
      log.error("Pattern analysis failed for executionPointId {}", executionPointId, e);
    }
  }

  private Optional<EuropeanaGeneratedIdsMap> getEuropeanaGeneratedIdsMap(String datasetId, String recordData) {
    EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
    try {
      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(recordData, datasetId);
    } catch (EuropeanaIdException e) {
      log.debug("Reading edm ids failed(probably not edm format), proceed without them", e);
    }
    return ofNullable(europeanaGeneratedIdsMap);
  }

  /**
   * A record that encapsulates the result of a validation process along with optional identifiers generated by Europeana's ID
   * creation service.
   *
   * @param validationResult the result of the validation process
   * @param europeanaGeneratedIdsMap an optional map of identifiers generated during the validation process
   */
  public record ValidationResultWithIdentifiers(ValidationResult validationResult,
                                                Optional<EuropeanaGeneratedIdsMap> europeanaGeneratedIdsMap) {

  }

  /**
   * Exception thrown when validation processes fail within the ValidationService.
   */
  @StandardException
  public static class ValidationException extends Exception {

  }
}

