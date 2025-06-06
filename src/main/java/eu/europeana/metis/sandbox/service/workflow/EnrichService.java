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
import org.springframework.stereotype.Service;

@Service
public class EnrichService {

  private final EnrichmentWorker enrichmentWorker;

  public EnrichService(EnrichmentWorker enrichmentWorker) {
    this.enrichmentWorker = enrichmentWorker;
  }

  public EnrichmentProcessingResult enrichRecord(String recordData) {
    try {
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

    } catch (Exception e) {
      throw new RuntimeException("Enrichment processing failed", e);
    }
  }

  private void handleRecordStopException(Set<Report> reports) {
    Optional<Report> errorReport = reports.stream()
                                          .filter(rep -> Objects.equals(rep.getMessageType(), Type.ERROR))
                                          .findFirst();
    String errorMessage = errorReport.map(this::createErrorMessage)
                                     .orElse("Something went wrong when processing record.");
    throw new ServiceException(errorMessage);
  }

  private String createErrorMessage(Report report) {
    return String.format("%s Value: %s", report.getMessage(), report.getValue());
  }

  public record EnrichmentProcessingResult(String processedRecord, List<ServiceException> warningExceptions) {}
}

