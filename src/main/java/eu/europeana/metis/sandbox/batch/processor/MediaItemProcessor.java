package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.MEDIA;
import static java.util.Objects.nonNull;

import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.RdfConverterFactory;
import eu.europeana.metis.mediaprocessing.RdfDeserializer;
import eu.europeana.metis.mediaprocessing.RdfSerializer;
import eu.europeana.metis.mediaprocessing.exception.MediaExtractionException;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mediaprocessing.exception.RdfDeserializationException;
import eu.europeana.metis.mediaprocessing.exception.RdfSerializationException;
import eu.europeana.metis.mediaprocessing.model.EnrichedRdf;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import eu.europeana.metis.mediaprocessing.model.ResourceExtractionResult;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@Component("mediaItemProcessor")
@StepScope
@Setter
public class MediaItemProcessor implements MetisItemProcessor<ExecutionRecord, ExecutionRecordDTO, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final BatchJobType batchJobType = MEDIA;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;

  private final ItemProcessorUtil<String> itemProcessorUtil;
  private final MediaExtractor mediaExtractor;
  private final RdfSerializer rdfSerializer;
  private final RdfDeserializer rdfDeserializer;

  public MediaItemProcessor() throws MediaProcessorException {
    final RdfConverterFactory rdfConverterFactory = new RdfConverterFactory();
    rdfDeserializer = rdfConverterFactory.createRdfDeserializer();
    rdfSerializer = rdfConverterFactory.createRdfSerializer();
    final MediaProcessorFactory mediaProcessorFactory = new MediaProcessorFactory();
    mediaExtractor = mediaProcessorFactory.createMediaExtractor();
    itemProcessorUtil = new ItemProcessorUtil<>(getFunction(), Function.identity());
  }

  @PreDestroy
  public void destroy() throws IOException {
    LOGGER.debug("Closing MediaExtractor");
    mediaExtractor.close();
  }

  @Override
  public ThrowingFunction<ExecutionRecordDTO, String> getFunction() {
    return executionRecord -> {
      LOGGER.info("MediaItemProcessor thread: {}", Thread.currentThread());
      final byte[] rdfBytes = executionRecord.getRecordData().getBytes(StandardCharsets.UTF_8);
      final EnrichedRdf enrichedRdf;
      enrichedRdf = getEnrichedRdf(rdfBytes);

      RdfResourceEntry resourceMainThumbnail;
      resourceMainThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(rdfBytes);
      boolean hasMainThumbnail = false;
      if (resourceMainThumbnail != null) {
        hasMainThumbnail = processResourceWithoutThumbnail(resourceMainThumbnail,
            executionRecord.getRecordId(), enrichedRdf, mediaExtractor);
      }
      List<RdfResourceEntry> remainingResourcesList;
      remainingResourcesList = rdfDeserializer.getRemainingResourcesForMediaExtraction(rdfBytes);
      if (hasMainThumbnail) {
        remainingResourcesList.forEach(entry ->
            processResourceWithThumbnail(entry, executionRecord.getRecordId(), enrichedRdf,
                mediaExtractor)
        );
      } else {
        remainingResourcesList.forEach(entry ->
            processResourceWithoutThumbnail(entry, executionRecord.getRecordId(), enrichedRdf,
                mediaExtractor)
        );
      }
      final byte[] outputRdfBytes;
      outputRdfBytes = getOutputRdf(enrichedRdf);
      return new String(outputRdfBytes, StandardCharsets.UTF_8);
    };
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final ExecutionRecordDTO executionRecordDTO = ExecutionRecordUtil.converterToExecutionRecordDTO(executionRecord);
    return itemProcessorUtil.processCapturingException(executionRecordDTO, batchJobType, targetExecutionId);
  }

  private EnrichedRdf getEnrichedRdf(byte[] rdfBytes) throws RdfDeserializationException {
    return rdfDeserializer.getRdfForResourceEnriching(rdfBytes);
  }

  private byte[] getOutputRdf(EnrichedRdf rdfForEnrichment) throws RdfSerializationException {
    return rdfSerializer.serialize(rdfForEnrichment);
  }

  private boolean processResourceWithThumbnail(RdfResourceEntry resourceToProcess, String recordId,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor) {
    return processResource(resourceToProcess, recordId, rdfForEnrichment, extractor, true);
  }

  private boolean processResourceWithoutThumbnail(RdfResourceEntry resourceToProcess, String recordId,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor) {
    return processResource(resourceToProcess, recordId, rdfForEnrichment, extractor, false);
  }

  private boolean processResource(RdfResourceEntry resourceToProcess, String recordId,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor, boolean gotMainThumbnail) {
    ResourceExtractionResult extraction;
    boolean successful = false;

    try {
      // Perform media extraction
      extraction = extractor.performMediaExtraction(resourceToProcess, gotMainThumbnail);

      // Check if extraction for media was successful
      successful = extraction != null;

      // If successful then store data
      if (successful) {
        rdfForEnrichment.enrichResource(extraction.getMetadata());
        if (!CollectionUtils.isEmpty(extraction.getThumbnails())) {
          storeThumbnails(recordId, extraction.getThumbnails());
        }
      }

    } catch (MediaExtractionException e) {
      LOGGER.warn("Error while extracting media for record {}. ", recordId, e);
    }

    return successful;
  }

  private void storeThumbnails(String recordId, List<Thumbnail> thumbnails) {
    if (nonNull(thumbnails)) {
      LOGGER.debug("Fake storing thumbnail");
    }
  }
}
