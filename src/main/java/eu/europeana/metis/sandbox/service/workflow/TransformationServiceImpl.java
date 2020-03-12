package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import org.springframework.stereotype.Service;

@Service
class TransformationServiceImpl implements TransformationService {

  @Override
  public Record transform(Record record) {
    return record;
  }
}
