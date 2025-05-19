package eu.europeana.metis.sandbox.batch.processor;

import java.util.function.Function;
import org.springframework.batch.item.ItemProcessor;

public interface MetisItemProcessor<I, O, R> extends ItemProcessor<I, O> {

  Function<O, R> getFunction();
}
