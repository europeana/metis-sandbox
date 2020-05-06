package eu.europeana.metis.sandbox.dto.report;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("ErrorInfo")
public class ErrorInfoDto {

  @JsonProperty("error-message")
  private String errorMessage;

  private Status status;

  @JsonProperty("records")
  private List<String> recordIds;

  public ErrorInfoDto(String errorMessage, Status status,
      List<String> recordIds) {
    requireNonNull(errorMessage, "Error message must not be null");
    requireNonNull(recordIds, "Record ids must not be null");
    requireNonNull(status, "Status must not be null");
    this.recordIds = recordIds;
    this.status = status;
    this.errorMessage = errorMessage;
  }

  public List<String> getRecordIds() {
    return recordIds;
  }

  public void setRecordIds(List<String> recordIds) {
    this.recordIds = recordIds;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
