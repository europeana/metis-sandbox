package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import eu.europeana.enrichment.rest.client.report.Report;
import eu.europeana.enrichment.rest.client.report.Type;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.experimental.StandardException;
import org.springframework.stereotype.Service;

/**
 * Service for processing and enriching data records using an {@link EnrichmentWorker}.
 */
@Service
public class EnrichService {

  private final EnrichmentWorker enrichmentWorker;

  /**
   * Constructor.
   *
   * @param enrichmentWorker the enrichment worker used to process and enrich data records
   */
  public EnrichService(EnrichmentWorker enrichmentWorker) {
    this.enrichmentWorker = enrichmentWorker;
  }

  /**
   * Enriches a data record.
   *
   * @param recordData the input record data to be processed and enriched
   * @return the result of the enrichment process, including the processed record and any warnings
   * @throws EnrichmentException if an error occurs
   */
  public EnrichmentProcessingResult enrichRecord(String recordData) throws EnrichmentException {
    ProcessedResult<String> processedResult = enrichmentWorker.process(recordData);
    Set<Report> reports = processedResult.getReport();

    if (processedResult.getRecordStatus() == ProcessedResult.RecordStatus.STOP) {
      handleRecordStopException(reports);
    }

    List<ServiceException> warningExceptions =
        reports.stream()
               .filter(report -> Objects.equals(report.getMessageType(), Type.WARN))
               .map(report -> new ServiceException(createErrorMessage(report), null))
               .toList();

    return new EnrichmentProcessingResult(processedResult.getProcessedRecord(), warningExceptions);
  }

  private void handleRecordStopException(Set<Report> reports) throws EnrichmentException {
    Optional<Report> errorReport = reports.stream()
                                          .filter(rep -> Objects.equals(rep.getMessageType(), Type.ERROR))
                                          .findFirst();
    String errorMessage = errorReport.map(this::createErrorMessage)
                                     .orElse("Something went wrong when processing record.");
    throw new EnrichmentException(errorMessage);
  }

  private String createErrorMessage(Report report) {
    return String.format("%s Value: %s", report.getMessage(), report.getValue());
  }

  /**
   * Represents the result of an enrichment processing operation.
   *
   * <p>Contains the processed record as a string and a list of any warning exceptions
   * encountered during the processing.
   *
   * @param processedRecord the processed data record after enrichment
   * @param warningExceptions list of warning exceptions encountered during processing
   */
  public record EnrichmentProcessingResult(String processedRecord, List<ServiceException> warningExceptions) {

  }

  /**
   * Exception thrown when an error occurs during the enrichment process.
   */
  @StandardException
  public static class EnrichmentException extends Exception {

  }
}

