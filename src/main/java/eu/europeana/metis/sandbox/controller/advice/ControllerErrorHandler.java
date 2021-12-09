package eu.europeana.metis.sandbox.controller.advice;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.XsltProcessingException;
import eu.europeana.metis.sandbox.dto.ExceptionModelDto;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Handles controller exceptions to report correct http status code to client
 */
@ControllerAdvice
class ControllerErrorHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ControllerErrorHandler.class);

  private static final String RETRY_MSG = "%s Please retry, if problem persists contact provider.";

  @ExceptionHandler(XsltProcessingException.class)
  public ResponseEntity<Object> handleXsltProcessingException(XsltProcessingException ex) {
    var message = format(RETRY_MSG, ex.getMessage());
    var exceptionModel = new ExceptionModelDto(HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR, message);
    LOGGER.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST, ex.getMessage());
    LOGGER.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(InvalidZipFileException.class)
  public ResponseEntity<Object> handleIInvalidZipFileException(InvalidZipFileException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST, ex.getMessage());
    LOGGER.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<Object> handleServiceException(ServiceException ex) {
    var message = format(RETRY_MSG, ex.getMessage());
    var exceptionModel = new ExceptionModelDto(HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR, message);
    LOGGER.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(RecordParsingException.class)
  public ResponseEntity<Object> handleNonRecoverableServiceException(
      RecordParsingException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST, ex.getMessage());
    LOGGER.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(InvalidDatasetException.class)
  public ResponseEntity<Object> handleInvalidDatasetException(
      InvalidDatasetException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST, ex.getMessage());
    LOGGER.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }
}
