package eu.europeana.metis.sandbox.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@ApiModel("ExceptionModel")
@RequiredArgsConstructor
@ToString
@Getter
public class ExceptionModelDto {

  @ApiModelProperty(allowableValues = "400,404,500")
  private final int statusCode;

  @ApiModelProperty(allowableValues = "400 BAD_REQUEST,404 NOT_FOUND,500 INTERNAL_SERVER_ERROR")
  private final HttpStatus status;

  @ApiModelProperty
  private final String message;
}