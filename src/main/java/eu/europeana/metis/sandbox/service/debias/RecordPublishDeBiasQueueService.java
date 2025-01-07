package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * The type Record publish DeBias queue service.
 */
@Service
public class RecordPublishDeBiasQueueService implements RecordDeBiasPublishable {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordPublishDeBiasQueueService.class);

  private final AmqpTemplate amqpTemplate;
  private final String deBiasReadyQueue;

  /**
   * Instantiates a new Record publish de bias queue service.
   *
   * @param amqpTemplate the amqp template
   * @param deBiasReadyQueue the de bias ready queue
   */
  public RecordPublishDeBiasQueueService(AmqpTemplate amqpTemplate,
      @Qualifier("deBiasReadyQueue") String deBiasReadyQueue) {
    this.amqpTemplate = amqpTemplate;
    this.deBiasReadyQueue = deBiasReadyQueue;
  }

  /**
   * Publish to de bias queue.
   *
   * @param recordToAnalyse the record to publish
   */
  @Override
  public void publishToDeBiasQueue(RecordInfo recordToAnalyse) {
    try {
      amqpTemplate.convertAndSend(deBiasReadyQueue, new RecordProcessEvent(recordToAnalyse, Step.DEBIAS, Status.PENDING));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordToAnalyse.getRecordValue().getRecordId(), e);
    }
  }
}
