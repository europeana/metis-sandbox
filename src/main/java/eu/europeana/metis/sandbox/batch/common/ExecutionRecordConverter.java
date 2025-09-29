package eu.europeana.metis.sandbox.batch.common;

import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.ExceptionInfoDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.Execution;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarning;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for converting ExecutionRecord and DTO objects.
 */
@UtilityClass
public final class ExecutionRecordConverter {

  /**
   * Converts an ExecutionRecord entity to a SuccessExecutionRecordDTO object.
   *
   * @param executionRecord The ExecutionRecord to be converted.
   * @return The corresponding SuccessExecutionRecordDTO object.
   */
  public static SuccessExecutionRecordDTO convertToExecutionRecordDTO(ExecutionRecord executionRecord) {
    Set<ExceptionInfoDTO> exceptionInfoDTOs = executionRecord.getExecutionRecordWarning().stream()
                                                             .map(executionRecordWarning -> new ExceptionInfoDTO(
                                                                 executionRecordWarning.getMessage(),
                                                                 executionRecordWarning.getException()))
                                                             .collect(Collectors.toSet());
    return SuccessExecutionRecordDTO.createValidated(builder -> builder
        .datasetId(executionRecord.getExecution().getDatasetId())
        .externalRecordId(executionRecord.getExternalRecordId())
        .sourceRecordId(executionRecord.getSourceRecordId())
        .recordId(executionRecord.getRecordId())
        .executionId(executionRecord.getExecution().getExecutionId())
        .executionName(executionRecord.getExecution().getExecutionName())
        .recordData(executionRecord.getRecordData())
        .exceptionWarnings(exceptionInfoDTOs));
  }

  /**
   * Converts a SuccessExecutionRecordDTO object to an ExecutionRecord object.
   *
   * @param executionRecordDTO The SuccessExecutionRecordDTO object containing execution record data to be converted.
   * @param execution
   * @return The converted ExecutionRecord object.
   */
  public static ExecutionRecord convertToExecutionRecord(SuccessExecutionRecordDTO executionRecordDTO, Execution execution) {
//    Execution execution = getExecution(executionRecordDTO);

    final ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setExecution(execution);
    executionRecord.setExternalRecordId(executionRecordDTO.getExternalRecordId());
    executionRecord.setSourceRecordId(executionRecordDTO.getSourceRecordId());
    executionRecord.setRecordId(executionRecordDTO.getRecordId());
    executionRecord.setRecordData(executionRecordDTO.getRecordData());

    List<ExecutionRecordWarning> executionRecordWarnings = new ArrayList<>();
    for (ExceptionInfoDTO exceptionInfoDTO : executionRecordDTO.getExceptionWarnings()) {
      ExecutionRecordWarning executionRecordWarning = new ExecutionRecordWarning();
      executionRecordWarning.setMessage(exceptionInfoDTO.getMessage());
      executionRecordWarning.setException(exceptionInfoDTO.getStackTrace());
      executionRecordWarning.setExecutionRecord(executionRecord);
      executionRecordWarnings.add(executionRecordWarning);
    }
    executionRecord.setExecutionRecordWarning(executionRecordWarnings);
    return executionRecord;
  }

  /**
   * Converts a SuccessExecutionRecordDTO object into an Optional containing an ExecutionRecordTierContext.
   *
   * @param successExecutionRecordDTO The input object containing tier results and relevant execution record details.
   * @param execution
   * @return An Optional containing the ExecutionRecordTierContext if tier fields are present, otherwise an empty Optional.
   */
  public static Optional<ExecutionRecordTierContext> convertToExecutionRecordTierContext(
      SuccessExecutionRecordDTO successExecutionRecordDTO, Execution execution) {
    final TierResults tierResults = successExecutionRecordDTO.getTierResults();

    final boolean containsTierFields =
        tierResults != null && tierResults.getMediaTier() != null && tierResults.getMetadataTier() != null;

    final Optional<ExecutionRecordTierContext> result;
    if (containsTierFields) {
//      Execution execution = getExecution(successExecutionRecordDTO);

      ExecutionRecordTierContext executionRecordTierContext = new ExecutionRecordTierContext();
      executionRecordTierContext.setExecution(execution);
      executionRecordTierContext.setExternalRecordId(successExecutionRecordDTO.getExternalRecordId());
      executionRecordTierContext.setSourceRecordId(successExecutionRecordDTO.getSourceRecordId());
      executionRecordTierContext.setRecordId(successExecutionRecordDTO.getRecordId());

      executionRecordTierContext.setContentTier(tierResults.getMediaTier().toString());
      executionRecordTierContext.setContentTierBeforeLicenseCorrection(
          tierResults.getContentTierBeforeLicenseCorrection().toString());
      executionRecordTierContext.setMetadataTier(tierResults.getMetadataTier().toString());
      executionRecordTierContext.setMetadataTierLanguage(tierResults.getMetadataTierLanguage().toString());
      executionRecordTierContext.setMetadataTierEnablingElements(tierResults.getMetadataTierEnablingElements().toString());
      executionRecordTierContext.setMetadataTierContextualClasses(tierResults.getMetadataTierContextualClasses().toString());
      executionRecordTierContext.setLicense(tierResults.getLicenseType().toString());

      result = Optional.of(executionRecordTierContext);
    } else {
      result = Optional.empty();
    }

    return result;
  }

  /**
   * Constructs an ExecutionRecordIdentifierKey object based on the provided ExecutionRecordDTO.
   *
   * @param executionRecordDTO The ExecutionRecordDTO containing the data to populate the ExecutionRecordIdentifierKey.
   * @return A populated ExecutionRecordIdentifierKey instance derived from the input ExecutionRecordDTO.
   */
  private static @NotNull Execution getExecution(
      AbstractExecutionRecordDTO executionRecordDTO) {
    Execution execution = new Execution();
    execution.setDatasetId(executionRecordDTO.getDatasetId());
    execution.setExecutionId(executionRecordDTO.getExecutionId());
    execution.setExecutionName(executionRecordDTO.getExecutionName());
    return execution;
  }

  /**
   * Converts a FailExecutionRecordDTO object into an ExecutionRecordException entity.
   *
   * @param failExecutionRecordDTO The FailExecutionRecordDTO containing failure details to be converted.
   * @param execution
   * @return An ExecutionRecordException entity reflecting the input data.
   */
  public static ExecutionRecordError converterToExecutionRecordError(FailExecutionRecordDTO failExecutionRecordDTO,
      Execution execution) {
//    Execution execution = getExecution(failExecutionRecordDTO);

    final ExecutionRecordError executionRecordError = new ExecutionRecordError();
    executionRecordError.setExecution(execution);
    executionRecordError.setExternalRecordId(failExecutionRecordDTO.getExternalRecordId());
    executionRecordError.setSourceRecordId(failExecutionRecordDTO.getSourceRecordId());
    executionRecordError.setRecordId(failExecutionRecordDTO.getRecordId());

    if (failExecutionRecordDTO.getExceptionInfoDTO() != null) {
      executionRecordError.setMessage(failExecutionRecordDTO.getExceptionInfoDTO().getMessage());
      executionRecordError.setException(failExecutionRecordDTO.getExceptionInfoDTO().getStackTrace());
    }

    return executionRecordError;
  }
}
