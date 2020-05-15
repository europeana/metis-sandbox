package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.enrichment.rest.client.DereferenceOrEnrichException;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
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

    byte[] result;
    try {
      result = enrichmentWorker.process(record.getContentInputStream());
    } catch (DereferenceOrEnrichException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    return new RecordInfo(Record.from(record, result));
  }
}
