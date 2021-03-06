package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.schema.convert.SerializationException;
import java.util.LinkedList;
import java.util.List;
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
    byte[] result;
    try {
      result = enrichmentWorker.process(record.getContentInputStream());
    } catch (EnrichmentException|DereferenceException|SerializationException e) {
      result = record.getContent();
      recordErrors.add(new RecordError(new RecordProcessingException(record.getRecordId(), e)));
    }

    return new RecordInfo(Record.from(record, result), recordErrors);
  }
}
