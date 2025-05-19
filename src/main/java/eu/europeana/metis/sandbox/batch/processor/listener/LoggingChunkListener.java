package eu.europeana.metis.sandbox.batch.processor.listener;

import java.lang.invoke.MethodHandles;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Setter
public class LoggingChunkListener implements ChunkListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void beforeChunk(@NotNull ChunkContext context){
    LOGGER.info("Before chunk");
  }

  @Override
  public void afterChunk(@NotNull ChunkContext context){
    LOGGER.info("After chunk");
  }

  @Override
  public void afterChunkError(@NotNull ChunkContext context){
    LOGGER.info("After chunk error");
  }

}
