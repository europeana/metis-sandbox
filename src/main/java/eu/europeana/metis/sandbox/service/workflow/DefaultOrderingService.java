package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import org.springframework.stereotype.Service;

@Service
class DefaultOrderingService implements OrderingService {

  @Override
  public Record performOrdering(Record record) {
    return record;
  }
}
