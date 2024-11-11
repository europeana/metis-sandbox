package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.dto.report.DatasetLogDto;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface DatasetLogService {

  /**
   * Remove matching dataset id
   *
   * @param datasetId must not be null
   */
  void remove(String datasetId);

  /**
   * It saves the log into the database
   * @param datasetId The id of the dataset to save the log to
   * @param status The status of the log
   * @param message The message of the log
   * @param exception The stack trace in case the log should save an exception
   * @return It is a void method, therefore it returns nothing
   */
  @Transactional
  Void log(String datasetId, Status status, String message, Throwable exception);

  /**
   * It saves an exception log into the database
   * @param datasetId The id of the dataset to save this log to
   * @param exception The exception to dave in the log
   * @return It is a void exception, it returns nothing
   */
  @Transactional
  Void logException(String datasetId, Throwable exception);

  /**
   * Return a list of all logs associated to the given dataset id
   * @param datasetId The id of the dataset to get the logs from
   * @return A list of
   */
  List<DatasetLogDto> getAllLogs(String datasetId);
}
