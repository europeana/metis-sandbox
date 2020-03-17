package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Record;
import org.springframework.stereotype.Service;

@Service
class ExternalValidationServiceImpl implements ExternalValidationService {

  @Override
  public Record validate(Record record) {
    return Record.from(record, Step.VALIDATE_EXTERNAL);
  }
}
