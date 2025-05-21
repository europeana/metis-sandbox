package eu.europeana.metis.sandbox.batch.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionRecordDTO {

  private String datasetId;
  private String executionId;
  private String recordId;
  private String executionName;
  private String recordData;
  private String exceptionMessage;
  private String exception;
}
