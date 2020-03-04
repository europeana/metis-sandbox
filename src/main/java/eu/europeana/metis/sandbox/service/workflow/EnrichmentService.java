package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;

public interface EnrichmentService {

  Record enrich(Record record);
}
