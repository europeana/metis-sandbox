package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;
import java.util.Objects;

/**
 * Represent errors or warnings in the dataset report
 */
@ApiModel("DatasetLog")
public class DatasetLogDto {

  @JsonProperty("message")
  private final String message;

  private final Status type;

  public DatasetLogDto(String message, Status type) {
    this.type = type;
    this.message = message;
  }

  public Status getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DatasetLogDto that = (DatasetLogDto) o;
    return Objects.equals(message, that.message) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, type);
  }
}
