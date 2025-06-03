package eu.europeana.metis.sandbox.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.http.HttpStatus;

/**
 * Represent exception model to display when there are errors in an http request
 */
@ApiModel(ExceptionModelDTO.SWAGGER_MODEL_NAME)
public class ExceptionModelDTO {

  public static final String SWAGGER_MODEL_NAME = "ExceptionModel";

  @ApiModelProperty(allowableValues = "400,404,500")
  private final int statusCode;

  @ApiModelProperty(allowableValues = "400 BAD_REQUEST,404 NOT_FOUND,500 INTERNAL_SERVER_ERROR")
  private final HttpStatus status;

  @ApiModelProperty
  private final String message;

  public ExceptionModelDTO(int statusCode, HttpStatus status, String message) {
    this.statusCode = statusCode;
    this.status = status;
    this.message = message;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public HttpStatus getStatus() {
    return this.status;
  }

  public String getMessage() {
    return this.message;
  }
}
