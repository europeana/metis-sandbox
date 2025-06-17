package eu.europeana.metis.sandbox.controller.advice;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.common.exception.InvalidCompressedFileException;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.XsltProcessingException;
import eu.europeana.metis.sandbox.dto.ExceptionModelDTO;
import eu.europeana.metis.schema.convert.SerializationException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Handles controller exceptions to report correct http status code to client
 */
@ControllerAdvice
public class RestResponseExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String RETRY_MSG = "%s Please retry, if problem persists contact provider.";

  /**
   * Handles exceptions of type {@link TaskRejectedException} and returns an appropriate HTTP response.
   *
   * <p>Occurs when too many executions are requested from the task executor for spring batch.
   *
   * @param ex The TaskRejectedException thrown when a task is rejected.
   * @return A ResponseEntity containing the HTTP status code and error message.
   */
  @ExceptionHandler(TaskRejectedException.class)
  public ResponseEntity<Object> handleTaskRejected(TaskRejectedException ex) {
    HttpStatus status = resolveStatus(ex, HttpStatus.TOO_MANY_REQUESTS);
    String message = format(RETRY_MSG, ex.getMessage());
    return buildResponse(status, message, ex);
  }

  /**
   * Handles internal server errors caused by specific exceptions and returns an appropriate HTTP response.
   *
   * @param ex The exception that triggered the internal server error.
   * @return A ResponseEntity containing the HTTP status code and error message.
   */
  @ExceptionHandler({
      XsltProcessingException.class,
      ServiceException.class,
  })
  public ResponseEntity<Object> handleInternalServerError(Exception ex) {
    HttpStatus status = resolveStatus(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    String message = format(RETRY_MSG, ex.getMessage());
    return buildResponse(status, message, ex);
  }

  /**
   * Handles exceptions of type {@link NoRecordFoundException} and returns an appropriate HTTP response.
   *
   * @param ex The NoRecordFoundException thrown when no record is found.
   * @return A ResponseEntity containing the HTTP status code and error message.
   */
  @ExceptionHandler(NoRecordFoundException.class)
  public ResponseEntity<Object> handleNoRecordFoundException(
      NoRecordFoundException ex) {
    HttpStatus status = resolveStatus(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    return buildResponse(status, ex.getMessage(), ex);
  }

  /**
   * Handles exceptions related to bad requests and returns an appropriate HTTP response.
   *
   * @param ex The exception that was triggered and caused the bad request.
   * @return A ResponseEntity containing the HTTP status code and error message.
   */
  @ExceptionHandler({
      IllegalArgumentException.class,
      InvalidCompressedFileException.class,
      RecordParsingException.class,
      InvalidDatasetException.class,
      SerializationException.class,
      IOException.class
  })
  public ResponseEntity<Object> handleBadRequestExceptions(Exception ex) {
    HttpStatus status = resolveStatus(ex, HttpStatus.BAD_REQUEST);
    return buildResponse(status, ex.getMessage(), ex);
  }

  private ResponseEntity<Object> buildResponse(HttpStatus status, String message, Exception ex) {
    LOGGER.error(message, ex);
    var dto = new ExceptionModelDTO(status.value(), status, message);
    return new ResponseEntity<>(dto, status);
  }

  private HttpStatus resolveStatus(Exception ex, HttpStatus defaultStatus) {
    ResponseStatus annotation = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
    return annotation != null ? annotation.value() : defaultStatus;
  }
}
