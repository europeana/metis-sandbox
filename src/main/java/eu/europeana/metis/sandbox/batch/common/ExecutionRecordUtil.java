package eu.europeana.metis.sandbox.batch.common;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExceptionLog;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarningExceptionLog;

public class ExecutionRecordUtil {

  private ExecutionRecordUtil() {
  }

  public static ExecutionRecordDTO converterToExecutionRecordDTO(ExecutionRecord executionRecord){
    final ExecutionRecordDTO executionRecordDTO = new ExecutionRecordDTO();
    executionRecordDTO.setDatasetId(executionRecord.getIdentifier().getDatasetId());
    executionRecordDTO.setExecutionId(executionRecord.getIdentifier().getExecutionId());
    executionRecordDTO.setRecordId(executionRecord.getIdentifier().getRecordId());
    executionRecordDTO.setExecutionName(executionRecord.getExecutionName());
    executionRecordDTO.setRecordData(executionRecord.getRecordData());
    return executionRecordDTO;
  }

  public static ExecutionRecord converterToExecutionRecord(ExecutionRecordDTO executionRecordDTO){
    ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
    executionRecordIdentifier.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordIdentifier.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordIdentifier.setRecordId(executionRecordDTO.getRecordId());
    final ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setIdentifier(executionRecordIdentifier);
    executionRecord.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecord.setRecordData(executionRecordDTO.getRecordData());
    return executionRecord;
  }

  public static ExecutionRecordExceptionLog converterToExecutionRecordExceptionLog(ExecutionRecordDTO executionRecordDTO){
    ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
    executionRecordIdentifier.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordIdentifier.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordIdentifier.setRecordId(executionRecordDTO.getRecordId());
    final ExecutionRecordExceptionLog executionRecordExceptionLog = new ExecutionRecordExceptionLog();
    executionRecordExceptionLog.setIdentifier(executionRecordIdentifier);
    executionRecordExceptionLog.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecordExceptionLog.setMessage(executionRecordDTO.getExceptionMessage());
    executionRecordExceptionLog.setException(executionRecordDTO.getException());
    return executionRecordExceptionLog;
  }

  public static ExecutionRecordWarningExceptionLog converterToExecutionRecordWarningExceptionLog(ExecutionRecordDTO executionRecordDTO){
    ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
    executionRecordIdentifier.setDatasetId(executionRecordDTO.getDatasetId());
    executionRecordIdentifier.setExecutionId(executionRecordDTO.getExecutionId());
    executionRecordIdentifier.setRecordId(executionRecordDTO.getRecordId());
    final ExecutionRecordWarningExceptionLog executionRecordWarningExceptionLog = new ExecutionRecordWarningExceptionLog();
    executionRecordWarningExceptionLog.setIdentifier(executionRecordIdentifier);
    executionRecordWarningExceptionLog.setExecutionName(executionRecordDTO.getExecutionName());
    executionRecordWarningExceptionLog.setMessage(executionRecordDTO.getExceptionMessage());
    executionRecordWarningExceptionLog.setException(executionRecordDTO.getException());
    return executionRecordWarningExceptionLog;
  }

  public static ExecutionRecordDTO createSuccessExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String updatedRecordString,
      String executionName, String executionId){
    final ExecutionRecordDTO resultExecutionRecordDTO = new ExecutionRecordDTO();
    resultExecutionRecordDTO.setDatasetId(executionRecordDTO.getDatasetId());
    resultExecutionRecordDTO.setExecutionId(executionId);
    resultExecutionRecordDTO.setRecordId(executionRecordDTO.getRecordId());
    resultExecutionRecordDTO.setExecutionName(executionName);
    resultExecutionRecordDTO.setRecordData(updatedRecordString);
    resultExecutionRecordDTO.setExceptionMessage(executionRecordDTO.getExceptionMessage());
    resultExecutionRecordDTO.setException(executionRecordDTO.getException());
    return resultExecutionRecordDTO;
  }

  public static ExecutionRecordDTO createFailureExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String executionName,
      String executionId, String exceptionMessage, String exception){
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
