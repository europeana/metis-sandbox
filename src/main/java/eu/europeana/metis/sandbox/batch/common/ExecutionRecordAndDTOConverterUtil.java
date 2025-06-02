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

public class ExecutionRecordAndDTOConverterUtil {

  private ExecutionRecordAndDTOConverterUtil() {
  }

  public static SuccessExecutionRecordDTO converterToExecutionRecordDTO(ExecutionRecord executionRecord) {
    return SuccessExecutionRecordDTO.createValidated(b -> b
                                    .datasetId(executionRecord.getIdentifier().getDatasetId())
                                    .sourceRecordId(executionRecord.getIdentifier().getSourceRecordId())
                                    .recordId(executionRecord.getIdentifier().getRecordId())
                                    .executionId(executionRecord.getIdentifier().getExecutionId())
                                    .executionName(executionRecord.getIdentifier().getExecutionName())
                                    .recordData(executionRecord.getRecordData()));
  }

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

  private static @NotNull ExecutionRecordIdentifierKey getExecutionRecordIdentifier(ExecutionRecordDTO executionRecordDTO) {
    ExecutionRecordIdentifierKey executionRecordIdentifierKey = new ExecutionRecordIdentifierKey();
    executionRecordIdentifierKey.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordIdentifierKey.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordIdentifierKey.setSourceRecordId(executionRecordDTO.getSourceRecordId());
    executionRecordIdentifierKey.setRecordId(executionRecordDTO.getRecordId());
    executionRecordIdentifierKey.setExecutionName(executionRecordDTO.getExecutionName());
    return executionRecordIdentifierKey;
  }

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

  public static String formatException(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);
    return stringWriter.toString();
  }
}
