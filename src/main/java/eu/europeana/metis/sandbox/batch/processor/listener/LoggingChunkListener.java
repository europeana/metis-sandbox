package eu.europeana.metis.sandbox.batch.processor.listener;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

/**
 * This class is a listener for chunk lifecycle events in a Spring Batch process,
 * logging specific events such as before and after chunk execution.
 *
 * <p>Used to provide information about the processing of chunks in the batch workflow.
 */
@Slf4j
@StepScope
@Component
public class LoggingChunkListener<I, O> implements ChunkListener<I, O> {

  @Override
  public void beforeChunk(@NonNull Chunk<I> chunk) {
    log.info("Before chunk");
  }

  @Override
  public void afterChunk(@NonNull Chunk<O> chunk) {
    log.info("After chunk");
  }

  @Override
  public void onChunkError(@NonNull Exception exception, @NonNull Chunk<O> chunk) {
    log.info("On chunk error");
  }
}
