package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import eu.europeana.enrichment.rest.client.report.Report;
import eu.europeana.enrichment.rest.client.report.Type;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component("enrichmentItemProcessor")
@StepScope
@Setter
public class EnrichmentItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;

  private final ItemProcessorUtil itemProcessorUtil;
  private EnrichmentWorker enrichmentWorker;

  public EnrichmentItemProcessor(EnrichmentWorker enrichmentWorker) {
    itemProcessorUtil = new ItemProcessorUtil(processSuccessRecord());
    this.enrichmentWorker = enrichmentWorker;
  }

  @Override
  public ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> processSuccessRecord() {
    return successExecutionRecordDTO -> {
      ProcessedResult<String> processedResult = enrichmentWorker.process(successExecutionRecordDTO.getRecordData());
      Set<Report> reports = processedResult.getReport();

      if (processedResult.getRecordStatus().equals(ProcessedResult.RecordStatus.STOP)) {
        handleRecordStopException(reports);
      }

      List<ServiceException> warningExceptions =
          reports.stream().filter(report -> Objects.equals(report.getMessageType(), Type.WARN))
                 .map(report -> new ServiceException(createErrorMessage(report), null))
                 .toList();

      return successExecutionRecordDTO.toBuilderOnlyIdentifiers(targetExecutionId, getExecutionName())
                                      .recordData(processedResult.getProcessedRecord())
                                      .exceptionWarnings(new HashSet<>(warningExceptions))
                                      .build();
    };
  }

  private void handleRecordStopException(Set<Report> reports) {
    Optional<Report> errorReport = reports.stream().filter(rep -> Objects.equals(rep.getMessageType(), Type.ERROR)).findFirst();
    String errorMessage = errorReport.map(this::createErrorMessage).orElse("Something went wrong when processing record.");
    throw new ServiceException(errorMessage);
  }

  private String createErrorMessage(Report report) {
    return String.format("%s Value: %s", report.getMessage(), report.getValue());
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO successExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    return itemProcessorUtil.processCapturingException(successExecutionRecordDTO, targetExecutionId, getExecutionName());
  }

}
