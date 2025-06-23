package eu.europeana.metis.sandbox.config;

import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.RdfConverterFactory;
import eu.europeana.metis.mediaprocessing.RdfDeserializer;
import eu.europeana.metis.mediaprocessing.RdfSerializer;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up media processing beans.
 */
@Configuration
class MediaConfig {

  @Bean
  MediaExtractor mediaExtractor() throws MediaProcessorException {
    return new MediaProcessorFactory().createMediaExtractor();
  }

  @Bean
  RdfSerializer rdfSerializer() {
    return new RdfConverterFactory().createRdfSerializer();
  }

  @Bean
  RdfDeserializer rdfDeserializer() {
    return new RdfConverterFactory().createRdfDeserializer();
  }
}
