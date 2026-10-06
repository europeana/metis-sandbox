package eu.europeana.metis.sandbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Represent exception model to display when there are errors in an http request
 */
@Schema(name = ExceptionModelDTO.SWAGGER_MODEL_NAME)
@Getter
@AllArgsConstructor
public class ExceptionModelDTO {

  public static final String SWAGGER_MODEL_NAME = "ExceptionModel";

  @Schema(
      description = "HTTP status code",
      example = "400",
      allowableValues = {"400", "404", "500"}
  )
  private final int statusCode;

  @Schema(
      description = "HTTP status enum",
      example = "BAD_REQUEST",
      allowableValues = {
          "400 BAD_REQUEST",
          "404 NOT_FOUND",
          "500 INTERNAL_SERVER_ERROR"
      }
  )
  private final HttpStatus status;

  @Schema
  private final String message;
}
