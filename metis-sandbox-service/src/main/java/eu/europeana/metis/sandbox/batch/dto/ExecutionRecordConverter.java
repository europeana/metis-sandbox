package eu.europeana.metis.sandbox.batch.dto;

import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
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
        .datasetId(executionRecord.getExecutionRun().getDatasetId())
        .externalRecordId(executionRecord.getIdentifier().getExternalRecordId())
        .sourceRecordId(executionRecord.getIdentifier().getSourceRecordId())
        .recordId(executionRecord.getIdentifier().getRecordId())
        .executionId(executionRecord.getExecutionRun().getExecutionId())
        .executionName(executionRecord.getExecutionRun().getExecutionName())
        .recordData(executionRecord.getRecordData())
        .exceptionWarnings(exceptionInfoDTOs));
  }

  /**
   * Converts a SuccessExecutionRecordDTO object to an ExecutionRecord object.
   *
   * @param executionRecordDTO The SuccessExecutionRecordDTO object containing execution record data to be converted.
   * @param executionRun the execution run for which the record is created.
   * @return The converted ExecutionRecord object.
   */
  public static ExecutionRecord convertToExecutionRecord(SuccessExecutionRecordDTO executionRecordDTO, ExecutionRun executionRun) {
    final ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setExecutionRun(executionRun);

    ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
    executionRecordIdentifier.setExternalRecordId(executionRecordDTO.getExternalRecordId());
    executionRecordIdentifier.setSourceRecordId(executionRecordDTO.getSourceRecordId());
    executionRecordIdentifier.setRecordId(executionRecordDTO.getRecordId());
    executionRecord.setIdentifier(executionRecordIdentifier);

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
   * @param executionRun the execution run for which the record tier context is created.
   * @return An Optional containing the ExecutionRecordTierContext if tier fields are present, otherwise an empty Optional.
   */
  public static Optional<ExecutionRecordTierContext> convertToExecutionRecordTierContext(
      SuccessExecutionRecordDTO successExecutionRecordDTO, ExecutionRun executionRun) {
    final TierResults tierResults = successExecutionRecordDTO.getTierResults();

    final boolean containsTierFields =
        tierResults != null && tierResults.getMediaTier() != null && tierResults.getMetadataTier() != null;

    final Optional<ExecutionRecordTierContext> result;
    if (containsTierFields) {

      ExecutionRecordTierContext executionRecordTierContext = new ExecutionRecordTierContext();
      executionRecordTierContext.setExecutionRun(executionRun);

      ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
      executionRecordIdentifier.setExternalRecordId(successExecutionRecordDTO.getExternalRecordId());
      executionRecordIdentifier.setSourceRecordId(successExecutionRecordDTO.getSourceRecordId());
      executionRecordIdentifier.setRecordId(successExecutionRecordDTO.getRecordId());
      executionRecordTierContext.setIdentifier(executionRecordIdentifier);

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
  private static @NotNull ExecutionRun getExecution(
      AbstractExecutionRecordDTO executionRecordDTO) {
    ExecutionRun executionRun = new ExecutionRun();
    executionRun.setDatasetId(executionRecordDTO.getDatasetId());
    executionRun.setExecutionId(executionRecordDTO.getExecutionId());
    executionRun.setExecutionName(executionRecordDTO.getExecutionName());
    return executionRun;
  }

  /**
   * Converts a FailExecutionRecordDTO object into an ExecutionRecordException entity.
   *
   * @param failExecutionRecordDTO The FailExecutionRecordDTO containing failure details to be converted.
   * @param executionRun the execution run for which the record is created.
   * @return An ExecutionRecordException entity reflecting the input data.
   */
  public static ExecutionRecordError converterToExecutionRecordError(FailExecutionRecordDTO failExecutionRecordDTO,
      ExecutionRun executionRun) {

    final ExecutionRecordError executionRecordError = new ExecutionRecordError();
    executionRecordError.setExecutionRun(executionRun);

    ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
    executionRecordIdentifier.setExternalRecordId(failExecutionRecordDTO.getExternalRecordId());
    executionRecordIdentifier.setSourceRecordId(failExecutionRecordDTO.getSourceRecordId());
    executionRecordIdentifier.setRecordId(failExecutionRecordDTO.getRecordId());
    executionRecordError.setIdentifier(executionRecordIdentifier);

    if (failExecutionRecordDTO.getExceptionInfoDTO() != null) {
      executionRecordError.setMessage(failExecutionRecordDTO.getExceptionInfoDTO().getMessage());
      executionRecordError.setException(failExecutionRecordDTO.getExceptionInfoDTO().getStackTrace());
    }

    return executionRecordError;
  }
}
