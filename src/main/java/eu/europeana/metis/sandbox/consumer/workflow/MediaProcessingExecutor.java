package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.MediaProcessingService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes enriched events and performs media processing to the contained record <br/> Publishes
 * the result in the media processed queue
 */
@Component
class MediaProcessingExecutor extends StepExecutor {

  private final MediaProcessingService service;

  @Value("${sandbox.rabbitmq.queues.record.media.queue}")
  private String routingKey;

  public MediaProcessingExecutor(AmqpTemplate amqpTemplate,
      MediaProcessingService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.enriched.queue}", containerFactory = "enrichedFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.enriched.auto-start:true}")
  public void processMedia(Event input) {
    consume(routingKey, input, Step.MEDIA_PROCESS, () -> service.processMedia(input.getBody()));
  }
}
