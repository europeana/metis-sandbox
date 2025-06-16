package eu.europeana.metis.sandbox.batch.common;

import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordException;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarningException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for converting ExecutionRecord and DTO objects.
 */
public final class ExecutionRecordAndDTOConverterUtil {

  private ExecutionRecordAndDTOConverterUtil() {
  }

  /**
   * Converts an ExecutionRecord entity to a SuccessExecutionRecordDTO object.
   *
   * @param executionRecord The ExecutionRecord to be converted.
   * @return The corresponding SuccessExecutionRecordDTO object.
   */
  public static SuccessExecutionRecordDTO converterToExecutionRecordDTO(ExecutionRecord executionRecord) {
    return SuccessExecutionRecordDTO.createValidated(b -> b
        .datasetId(executionRecord.getIdentifier().getDatasetId())
        .sourceRecordId(executionRecord.getIdentifier().getSourceRecordId())
        .recordId(executionRecord.getIdentifier().getRecordId())
        .executionId(executionRecord.getIdentifier().getExecutionId())
        .executionName(executionRecord.getIdentifier().getExecutionName())
        .recordData(executionRecord.getRecordData()));
  }

  /**
   * Converts a SuccessExecutionRecordDTO object to an ExecutionRecord object.
   *
   * @param executionRecordDTO The SuccessExecutionRecordDTO object containing execution record data to be converted.
   * @return The converted ExecutionRecord object.
   */
  public static ExecutionRecord converterToExecutionRecord(SuccessExecutionRecordDTO executionRecordDTO) {
    ExecutionRecordIdentifierKey executionRecordIdentifierKey = getExecutionRecordIdentifier(executionRecordDTO);

    final ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setIdentifier(executionRecordIdentifierKey);
    executionRecord.setRecordData(executionRecordDTO.getRecordData());

    List<ExecutionRecordWarningException> executionRecordWarningExceptions = new ArrayList<>();
    for (Exception exception : executionRecordDTO.getExceptionWarnings()) {
      ExecutionRecordWarningException executionRecordWarningException = new ExecutionRecordWarningException();
      executionRecordWarningException.setMessage(exception.getMessage());
      executionRecordWarningException.setException(formatException(exception));
      executionRecordWarningException.setExecutionRecord(executionRecord);
      executionRecordWarningExceptions.add(executionRecordWarningException);
    }
    executionRecord.setExecutionRecordWarningException(executionRecordWarningExceptions);
    return executionRecord;
  }

  /**
   * Converts a SuccessExecutionRecordDTO object into an Optional containing an ExecutionRecordTierContext.
   *
   * @param successExecutionRecordDTO The input object containing tier results and relevant execution record details.
   * @return An Optional containing the ExecutionRecordTierContext if tier fields are present, otherwise an empty Optional.
   */
  public static Optional<ExecutionRecordTierContext> converterToExecutionRecordTierContext(
      SuccessExecutionRecordDTO successExecutionRecordDTO) {
    TierResults tierResults = successExecutionRecordDTO.getTierResults();

    boolean containsTierFields =
        tierResults != null && tierResults.getMediaTier() != null && tierResults.getMetadataTier() != null;

    final Optional<ExecutionRecordTierContext> result;
    if (containsTierFields) {
      ExecutionRecordIdentifierKey executionRecordIdentifierKey = getExecutionRecordIdentifier(successExecutionRecordDTO);

      ExecutionRecordTierContext executionRecordTierContext = new ExecutionRecordTierContext();
      executionRecordTierContext.setIdentifier(executionRecordIdentifierKey);
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
  private static @NotNull ExecutionRecordIdentifierKey getExecutionRecordIdentifier(ExecutionRecordDTO executionRecordDTO) {
    ExecutionRecordIdentifierKey executionRecordIdentifierKey = new ExecutionRecordIdentifierKey();
    executionRecordIdentifierKey.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordIdentifierKey.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordIdentifierKey.setSourceRecordId(executionRecordDTO.getSourceRecordId());
    executionRecordIdentifierKey.setRecordId(executionRecordDTO.getRecordId());
    executionRecordIdentifierKey.setExecutionName(executionRecordDTO.getExecutionName());
    return executionRecordIdentifierKey;
  }

  /**
   * Converts a FailExecutionRecordDTO object into an ExecutionRecordException entity.
   *
   * @param failExecutionRecordDTO The FailExecutionRecordDTO containing failure details to be converted.
   * @return An ExecutionRecordException entity reflecting the input data.
   */
  public static ExecutionRecordException converterToExecutionRecordExceptionLog(
      FailExecutionRecordDTO failExecutionRecordDTO) {
    ExecutionRecordIdentifierKey executionRecordIdentifierKey = getExecutionRecordIdentifier(
        failExecutionRecordDTO);

    final ExecutionRecordException executionRecordException = new ExecutionRecordException();
    executionRecordException.setIdentifier(executionRecordIdentifierKey);

    if (failExecutionRecordDTO.getException() != null) {
      executionRecordException.setMessage(failExecutionRecordDTO.getException().getMessage());
      executionRecordException.setException(formatException(failExecutionRecordDTO.getException()));
    }

    return executionRecordException;
  }

  /**
   * Formats the stack trace of a Throwable into a String.
   *
   * @param throwable The Throwable whose stack trace will be formatted.
   * @return The formatted stack trace as a String.
   */
  private static String formatException(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);
    return stringWriter.toString();
  }
}
