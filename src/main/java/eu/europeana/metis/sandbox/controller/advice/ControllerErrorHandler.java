package eu.europeana.metis.sandbox.controller.advice;

import eu.europeana.metis.sandbox.common.exception.InvalidZipFileException;
import eu.europeana.metis.sandbox.common.exception.NonRecoverableServiceException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.ExceptionModelDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class ControllerErrorHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST, ex.getMessage());
    log.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(InvalidZipFileException.class)
  public ResponseEntity<Object> handleIInvalidZipFileException(InvalidZipFileException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST, ex.getMessage());
    log.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<Object> handleServiceException(ServiceException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.INTERNAL_SERVER_ERROR.value(),
        HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    log.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }

  @ExceptionHandler(NonRecoverableServiceException.class)
  public ResponseEntity<Object> handleNonRecoverableServiceException(NonRecoverableServiceException ex) {
    var exceptionModel = new ExceptionModelDto(HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST, ex.getMessage());
    log.error(ex.getMessage(), ex);
    return new ResponseEntity<>(exceptionModel, exceptionModel.getStatus());
  }
}
