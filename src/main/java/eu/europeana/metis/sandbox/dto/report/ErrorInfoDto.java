package eu.europeana.metis.sandbox.dto.report;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("ErrorInfo")
public class ErrorInfoDto {

  @JsonProperty("error-message")
  private String errorMessage;

  @JsonProperty("records")
  private List<String> recordIds;

  public ErrorInfoDto(String errorMessage, List<String> recordIds) {
    requireNonNull(errorMessage, "Error message must not be null");
    requireNonNull(recordIds, "Record ids must not be null");
    this.recordIds = recordIds;
    this.errorMessage = errorMessage;
  }

  public List<String> getRecordIds() {
    return recordIds;
  }

  public void setRecordId(List<String> recordIds) {
    this.recordIds = recordIds;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
