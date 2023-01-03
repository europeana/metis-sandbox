package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
import eu.europeana.enrichment.rest.client.report.Report;
import eu.europeana.enrichment.rest.client.report.Type;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
class EnrichmentServiceImpl implements EnrichmentService {

    private final EnrichmentWorker enrichmentWorker;

    public EnrichmentServiceImpl(EnrichmentWorker enrichmentWorker) {
        this.enrichmentWorker = enrichmentWorker;
    }

    @Override
    public RecordInfo enrich(Record record) {
        requireNonNull(record, "Record must not be null");

        List<RecordError> recordErrors = new LinkedList<>();
        ProcessedResult<byte[]> result;
        try {
            result = enrichmentWorker.process(record.getContentInputStream());
            Set<Report> reports = result.getReport();

            if (result.getRecordStatus().equals(ProcessedResult.RecordStatus.STOP)) {
                Optional<Report> report = reports.stream().filter(rep -> Objects.equals(rep.getMessageType(), Type.ERROR)).findFirst();
                throw new RecordProcessingException(record.getProviderId(), handleRecordStopException(report));
            }

            reports.stream().filter(report -> report.getMessageType().equals(Type.WARN))
                    .forEach(report -> recordErrors.add(new RecordError(new RecordProcessingException(record.getProviderId(),
                            new ServiceException(handleErrorMessage(report), null)))));

        } catch (Exception e) {
            throw new RecordProcessingException(record.getProviderId(), e);
        }

        return new RecordInfo(Record.from(record, result.getProcessedRecord()), recordErrors);
    }

    private String handleErrorMessage(Report report) {
        return String.format("%s Value: %s", report.getMessage() ,report.getValue());
    }

    private Exception handleRecordStopException(Optional<Report> report) {
        return report.isPresent() ? new ServiceException(handleErrorMessage(report.get()),
                new Exception("StackTrace: " + report.get().getStackTrace())) :
                new ServiceException("Something went wrong when requesting report.", null);
    }
}
