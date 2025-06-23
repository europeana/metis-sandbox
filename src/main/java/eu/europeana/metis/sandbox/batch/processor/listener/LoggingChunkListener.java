package eu.europeana.metis.sandbox.batch.processor.listener;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
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
public class LoggingChunkListener implements ChunkListener {

  @Override
  public void beforeChunk(@NotNull ChunkContext context){
    log.info("Before chunk");
  }

  @Override
  public void afterChunk(@NotNull ChunkContext context){
    log.info("After chunk");
  }

  @Override
  public void afterChunkError(@NotNull ChunkContext context){
    log.info("After chunk error");
  }

}
