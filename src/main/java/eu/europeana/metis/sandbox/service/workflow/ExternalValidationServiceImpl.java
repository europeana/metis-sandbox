package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.TransformationException;
import org.springframework.stereotype.Service;

@Service
class ExternalValidationServiceImpl implements ExternalValidationService {

  private OrderingService orderingService;

  public ExternalValidationServiceImpl(
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
