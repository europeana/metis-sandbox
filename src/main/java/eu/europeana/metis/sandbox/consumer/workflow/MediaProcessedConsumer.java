package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.IndexEnvironment;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes media processed events and performs indexing to the contained record
 * <br/>
 * Publishes the result in the indexed queue
 */
@Component
class MediaProcessedConsumer extends StepConsumer {

  private final IndexingService service;

  @Value("${sandbox.rabbitmq.queues.record.previewed.queue}")
  private String routingKey;

  public MediaProcessedConsumer(AmqpTemplate amqpTemplate,
      IndexingService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.media.queue}", containerFactory = "mediaProcessedFactory")
  public void preview(Event input) {
    consume(routingKey, input, Step.PREVIEW,
        () -> service.index(input.getBody(), IndexEnvironment.PREVIEW));
  }
}
