package eu.europeana.metis.sandbox.common.exception;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the size of a download payload exceeds the allowed limit.
 */
@ResponseStatus(value = HttpStatus.CONTENT_TOO_LARGE, reason = "Payload size exceeded")
@StandardException
public class DownloadSizeExceededException extends RuntimeException {

}
