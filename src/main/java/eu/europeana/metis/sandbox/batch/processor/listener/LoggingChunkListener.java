package eu.europeana.metis.sandbox.batch.processor.listener;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

/**
 * Listener for logging events during the execution of a Spring Batch chunk.
 * <p>
 * Logs events before a chunk is processed, after a chunk is processed, and when an error occurs during chunk processing. Useful
 * for monitoring and debugging in batch processing workflows.
 *
 * @param <I> Type of input items in the chunk.
 * @param <O> Type of output items in the chunk.
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
