package eu.europeana.metis.sandbox.batch.common;

import static io.micrometer.common.util.StringUtils.isNotBlank;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExceptionLog;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarningException;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class ExecutionRecordUtil {

  private ExecutionRecordUtil() {
  }

  public static ExecutionRecordDTO converterToExecutionRecordDTO(ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
    executionRecordDTO.setDatasetId(executionRecord.getIdentifier().getDatasetId());
    executionRecordDTO.setExecutionId(executionRecord.getIdentifier().getExecutionId());
    executionRecordDTO.setRecordId(executionRecord.getIdentifier().getRecordId());
    executionRecordDTO.setExecutionName(executionRecord.getIdentifier().getExecutionName());
    executionRecordDTO.setRecordData(executionRecord.getRecordData());
    return executionRecordDTO;
  }

  public static ExecutionRecord converterToExecutionRecord(ExecutionRecordDTO executionRecordDTO) {
    ExecutionRecordIdentifier executionRecordIdentifier = getExecutionRecordIdentifier(executionRecordDTO);

    final ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setIdentifier(executionRecordIdentifier);
    executionRecord.setRecordData(executionRecordDTO.getRecordData());

    if (isNotBlank(executionRecordDTO.getExceptionMessage()) && isNotBlank(
        executionRecordDTO.getException())) {
      ExecutionRecordWarningException executionRecordWarningException = new ExecutionRecordWarningException();
      executionRecordWarningException.setMessage(executionRecordDTO.getExceptionMessage());
      executionRecordWarningException.setException(executionRecordDTO.getException());
      executionRecordWarningException.setExecutionRecord(executionRecord);
      executionRecord.setExecutionRecordWarningException(List.of(executionRecordWarningException));
    }
    return executionRecord;
  }

  public static Optional<ExecutionRecordTierContext> converterToExecutionRecordTierContext(
      ExecutionRecordDTO executionRecordDTO) {
    boolean containsTierFields =
        isNotBlank(executionRecordDTO.getContentTier()) && isNotBlank(executionRecordDTO.getMetadataTier());

    final Optional<ExecutionRecordTierContext> result;
    if (containsTierFields) {
      ExecutionRecordIdentifier executionRecordIdentifier = getExecutionRecordIdentifier(executionRecordDTO);

      ExecutionRecordTierContext executionRecordTierContext = new ExecutionRecordTierContext();
      executionRecordTierContext.setIdentifier(executionRecordIdentifier);
      executionRecordTierContext.setContentTier(executionRecordDTO.getContentTier());
      executionRecordTierContext.setContentTierBeforeLicenseCorrection(
          executionRecordDTO.getContentTierBeforeLicenseCorrection());
      executionRecordTierContext.setMetadataTier(executionRecordDTO.getMetadataTier());
      executionRecordTierContext.setMetadataTierLanguage(executionRecordDTO.getMetadataTierLanguage());
      executionRecordTierContext.setMetadataTierEnablingElements(executionRecordDTO.getMetadataTierEnablingElements());
      executionRecordTierContext.setMetadataTierContextualClasses(executionRecordDTO.getMetadataTierContextualClasses());
      executionRecordTierContext.setLicense(executionRecordDTO.getLicense());

      result = Optional.of(executionRecordTierContext);
    } else {
      result = Optional.empty();
    }

    return result;
  }

  private static @NotNull ExecutionRecordIdentifier getExecutionRecordIdentifier(ExecutionRecordDTO executionRecordDTO) {
    ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
    executionRecordIdentifier.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordIdentifier.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordIdentifier.setRecordId(executionRecordDTO.getRecordId());
    executionRecordIdentifier.setExecutionName(executionRecordDTO.getExecutionName());
    return executionRecordIdentifier;
  }

  public static ExecutionRecordExceptionLog converterToExecutionRecordExceptionLog(ExecutionRecordDTO executionRecordDTO) {
    ExecutionRecordIdentifier executionRecordIdentifier = getExecutionRecordIdentifier(
        executionRecordDTO);

    final ExecutionRecordExceptionLog executionRecordExceptionLog = new ExecutionRecordExceptionLog();
    executionRecordExceptionLog.setIdentifier(executionRecordIdentifier);
    executionRecordExceptionLog.setMessage(executionRecordDTO.getExceptionMessage());
    executionRecordExceptionLog.setException(executionRecordDTO.getException());
    return executionRecordExceptionLog;
  }

  public static ExecutionRecordDTO createSuccessExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO,
      String updatedRecordString,
      String executionName, String executionId) {
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    resultExecutionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    resultExecutionRecordDTO.setExecutionId(executionId);
    resultExecutionRecordDTO.setRecordId(executionRecordDTO.getRecordId());
    resultExecutionRecordDTO.setExecutionName(executionName);
    resultExecutionRecordDTO.setRecordData(updatedRecordString);
    resultExecutionRecordDTO.setExceptionMessage(executionRecordDTO.getExceptionMessage());
    resultExecutionRecordDTO.setException(executionRecordDTO.getException());
    resultExecutionRecordDTO.setContentTier(executionRecordDTO.getContentTier());
    resultExecutionRecordDTO.setContentTierBeforeLicenseCorrection(executionRecordDTO.getContentTierBeforeLicenseCorrection());
    resultExecutionRecordDTO.setMetadataTier(executionRecordDTO.getMetadataTier());
    resultExecutionRecordDTO.setMetadataTierLanguage(executionRecordDTO.getMetadataTierLanguage());
    resultExecutionRecordDTO.setMetadataTierEnablingElements(executionRecordDTO.getMetadataTierEnablingElements());
    resultExecutionRecordDTO.setMetadataTierContextualClasses(executionRecordDTO.getMetadataTierContextualClasses());
    resultExecutionRecordDTO.setLicense(executionRecordDTO.getLicense());
    return resultExecutionRecordDTO;
  }

  public static ExecutionRecordDTO createFailureExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String executionName,
      String executionId, String exceptionMessage, String exception) {
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    resultExecutionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    resultExecutionRecordDTO.setExecutionId(executionId);
    resultExecutionRecordDTO.setRecordId(executionRecordDTO.getRecordId());
    resultExecutionRecordDTO.setExecutionName(executionName);
    resultExecutionRecordDTO.setRecordData("");
    resultExecutionRecordDTO.setExceptionMessage(exceptionMessage);
    resultExecutionRecordDTO.setException(exception);
    return resultExecutionRecordDTO;
  }
}
