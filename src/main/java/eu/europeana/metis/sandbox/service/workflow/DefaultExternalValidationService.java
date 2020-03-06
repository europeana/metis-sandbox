package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.TransformationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class DefaultExternalValidationService implements ExternalValidationService {

  private OrderingService orderingService;

  public DefaultExternalValidationService(
      OrderingService orderingService) {
    this.orderingService = orderingService;
  }

  @Override
  public Record validate(Record record) {
    String recordOrdered;
    try {
      recordOrdered = orderingService.performOrdering(record.getContent());
    } catch (TransformationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    return Record.from(record, recordOrdered);
  }

}
