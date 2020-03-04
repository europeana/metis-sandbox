package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import org.springframework.stereotype.Service;

@Service
class DefaultNormalizationService implements NormalizationService {

  @Override
  public Record normalize(Record record) {
    return record;
  }
}
