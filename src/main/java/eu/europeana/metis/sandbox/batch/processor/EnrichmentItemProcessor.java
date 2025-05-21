package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.ENRICHMENT;
import static eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil.formatException;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferencerProvider;
import eu.europeana.enrichment.rest.client.enrichment.EnricherProvider;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import eu.europeana.enrichment.rest.client.report.Report;
import eu.europeana.enrichment.rest.client.report.Type;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component("enrichmentItemProcessor")
@StepScope
@Setter
public class EnrichmentItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, ProcessedResult<String>> {

  private static final BatchJobType batchJobType = ENRICHMENT;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("${sandbox.enrichment.dereference-url}")
  private String dereferenceURL;
  @Value("${sandbox.enrichment.enrichment-properties.entity-management-url}")
  private String enrichmentEntityManagementUrl;
  @Value("${sandbox.enrichment.enrichment-properties.entity-api-url}")
  private String enrichmentEntityApiUrl;
  @Value("${sandbox.enrichment.enrichment-properties.entity-api-token-endpoint}")
  private String enrichmentEntityApiTokenEndpoint;
  @Value("${sandbox.enrichment.enrichment-properties.entity-api-grant-params}")
  private String enrichmentEntityApiGrantParams;

  private final ItemProcessorUtil<ProcessedResult<String>> itemProcessorUtil;
  private EnrichmentWorker enrichmentWorker;

  public EnrichmentItemProcessor() {
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), ProcessedResult::getProcessedRecord);
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, ProcessedResult<String>> getFunction() {
    return executionRecordDTO -> {
      ProcessedResult<String> processedResult = enrichmentWorker.process(executionRecordDTO.getRecordData());
      //Add warnings
      executionRecordDTO.setRecordData(processedResult.getProcessedRecord());
      Set<Report> reports = processedResult.getReport();

      if (processedResult.getRecordStatus().equals(ProcessedResult.RecordStatus.STOP)) {
        handleRecordStopException(reports, executionRecordDTO.getRecordId());
      }

      List<RecordProcessingException> warningExceptions =
          reports.stream().filter(report -> Objects.equals(report.getMessageType(), Type.WARN))
                 .map(report ->
                     new RecordProcessingException(executionRecordDTO.getRecordId(),
                         new ServiceException(createErrorMessage(report), null)))
                 .toList();

      // Convert exceptions to a single formatted string with messages and stack traces
      String warningMessages = warningExceptions.stream().map(RecordProcessingException::getReportMessage)
                                                .collect(Collectors.joining("\n"));
      String exceptionsStacktraces = warningExceptions.stream()
                                                      .map(e -> formatException(
                                                          e.getCause())) // getCause() is the ServiceException
                                                      .collect(Collectors.joining("\n\n"));

      executionRecordDTO.setExceptionMessage(warningMessages);
      executionRecordDTO.setException(exceptionsStacktraces);
      return processedResult;
    };
  }

  private void handleRecordStopException(Set<Report> reports, String providerId) {
    Optional<Report> report = reports.stream().filter(rep -> Objects.equals(rep.getMessageType(), Type.ERROR)).findFirst();
    if (report.isPresent()) {
      throw new RecordProcessingException(providerId, new ServiceException(createErrorMessage(report.get()), null));
    } else {
      throw new RecordProcessingException(providerId, new ServiceException("Something went wrong when requesting report.", null));
    }
  }

  private String createErrorMessage(Report report) {
    return String.format("%s Value: %s", report.getMessage(), report.getValue());
  }

  @PostConstruct
  private void postConstruct() throws DereferenceException, EnrichmentException {
    final EnricherProvider enricherProvider = new EnricherProvider();
    enricherProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl, enrichmentEntityApiUrl,
        enrichmentEntityApiTokenEndpoint, enrichmentEntityApiGrantParams);
    final DereferencerProvider dereferencerProvider = new DereferencerProvider();
    dereferencerProvider.setDereferenceUrl(dereferenceURL);
    dereferencerProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl, enrichmentEntityApiUrl,
        enrichmentEntityApiTokenEndpoint, enrichmentEntityApiGrantParams);
    enrichmentWorker = new EnrichmentWorkerImpl(dereferencerProvider.create(), enricherProvider.create());
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, targetExecutionId);
  }

}
