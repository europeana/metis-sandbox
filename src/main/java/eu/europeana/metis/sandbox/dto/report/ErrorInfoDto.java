package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

/**
 * Represent errors in the dataset report
 */
@ApiModel("ErrorInfo")
public class ErrorInfoDto {

  @JsonProperty("message")
  private final String errorMessage;

  private final Status type;

  @JsonProperty("records")
  private final List<String> recordIds;

  public ErrorInfoDto(String errorMessage, Status type,
      List<String> recordIds) {
    this.recordIds = Collections.unmodifiableList(recordIds);
    this.type = type;
    this.errorMessage = errorMessage;
  }

  public List<String> getRecordIds() {
    return recordIds;
  }

  public Status getType() {
    return type;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
