package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.enrichment.rest.client.DereferenceOrEnrichException;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import org.jibx.runtime.JiBXException;
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
    String result;
    try {
      result = enrichmentWorker.process(record.getContentString());
    } catch (DereferenceOrEnrichException | JiBXException | UnsupportedEncodingException e) {
      result = record.getContentString();
      recordErrors.add(new RecordError(new RecordProcessingException(record.getRecordId(), e)));
    }

    return new RecordInfo(Record.from(record, result), recordErrors);
  }
}
