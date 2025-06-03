package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;

/**
 * Represent errors or warnings in the dataset report
 */
@ApiModel("DatasetLog")
public class DatasetLogDTO {

  @JsonProperty("message")
  private final String message;

  private final Status type;

  public DatasetLogDTO(String message, Status type) {
    this.type = type;
    this.message = message;
  }

  public Status getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

}
