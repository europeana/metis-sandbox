package eu.europeana.metis.sandbox.dto.report;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("ErrorInfo")
public class ErrorInfoDto {

  @JsonProperty("message")
  private String errorMessage;

  private Status type;

  @JsonProperty("records")
  private List<String> recordIds;

  public ErrorInfoDto(String errorMessage, Status type,
      List<String> recordIds) {
    requireNonNull(errorMessage, "Error message must not be null");
    requireNonNull(type, "Status must not be null");
    requireNonNull(recordIds, "Record ids must not be null");
    this.recordIds = recordIds;
    this.type = type;
    this.errorMessage = errorMessage;
  }

  public List<String> getRecordIds() {
    return recordIds;
  }

  public void setRecordIds(List<String> recordIds) {
    this.recordIds = recordIds;
  }

  public Status getType() {
    return type;
  }

  public void setType(Status type) {
    this.type = type;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
