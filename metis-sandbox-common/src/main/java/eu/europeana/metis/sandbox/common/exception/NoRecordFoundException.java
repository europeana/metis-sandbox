package eu.europeana.metis.sandbox.common.exception;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception for when a record is not found
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No record found")
@StandardException
public class NoRecordFoundException extends Exception {

}
