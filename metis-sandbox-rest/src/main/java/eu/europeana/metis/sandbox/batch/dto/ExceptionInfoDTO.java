package eu.europeana.metis.sandbox.batch.dto;

import java.io.PrintWriter;
import java.io.StringWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data Transfer Object that encapsulates exception details including the message and stack trace.
 *
 * <p>Primarily used for transferring exception information in structured formats.
 */
@Getter
@AllArgsConstructor
public class ExceptionInfoDTO {

  private final String message;
  private final String stackTrace;

  /**
   * Converts the given Throwable into an ExceptionInfoDTO instance.
   *
   * <p>This method extracts the message and stack trace from the provided Throwable
   * and encapsulates them into an ExceptionInfoDTO for structured exception representation.
   *
   * @param throwable the throwable from which exception information is to be extracted
   * @return an ExceptionInfoDTO containing the message and stack trace of the provided throwable
   */
  public static ExceptionInfoDTO from(Throwable throwable) {
    return new ExceptionInfoDTO(
        throwable.getMessage(),
        getStackTraceAsString(throwable)
    );
  }

  private static String getStackTraceAsString(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);
    return stringWriter.toString();
  }
}
