package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.RdfDeserializer;
import eu.europeana.metis.mediaprocessing.RdfSerializer;
import eu.europeana.metis.mediaprocessing.exception.MediaExtractionException;
import eu.europeana.metis.mediaprocessing.exception.RdfDeserializationException;
import eu.europeana.metis.mediaprocessing.exception.RdfSerializationException;
import eu.europeana.metis.mediaprocessing.model.EnrichedRdf;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import eu.europeana.metis.mediaprocessing.model.ResourceExtractionResult;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Setter;
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
public class MediaItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;

  private final ItemProcessorUtil itemProcessorUtil;
  private final MediaExtractor mediaExtractor;
  private final ThumbnailStoreService thumbnailStoreService;
  private final RdfSerializer rdfSerializer;
  private final RdfDeserializer rdfDeserializer;

  public MediaItemProcessor(MediaExtractor mediaExtractor, ThumbnailStoreService thumbnailStoreService,
      RdfSerializer rdfSerializer, RdfDeserializer rdfDeserializer) {
    this.itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
    this.mediaExtractor = mediaExtractor;
    this.thumbnailStoreService = thumbnailStoreService;
    this.rdfSerializer = rdfSerializer;
    this.rdfDeserializer = rdfDeserializer;
  }

  @Override
  public ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return originSuccessExecutionRecordDTO -> {
      LOGGER.debug("MediaItemProcessor thread: {}", Thread.currentThread());
      final byte[] rdfBytes = originSuccessExecutionRecordDTO.getRecordData().getBytes(StandardCharsets.UTF_8);
      final EnrichedRdf enrichedRdf = getEnrichedRdf(rdfBytes);

      RdfResourceEntry resourceMainThumbnail;
      resourceMainThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(rdfBytes);
      boolean hasMainThumbnail = false;
      List<Exception> warningExceptions = new ArrayList<>();
      if (resourceMainThumbnail != null) {
        safeProcessResource(
            entry -> processResourceWithoutThumbnail(entry,
                enrichedRdf, mediaExtractor, originSuccessExecutionRecordDTO.getDatasetId()), List.of(resourceMainThumbnail),
            warningExceptions);
        hasMainThumbnail = warningExceptions.isEmpty();
      }
      List<RdfResourceEntry> remainingResourcesList;
      remainingResourcesList = rdfDeserializer.getRemainingResourcesForMediaExtraction(rdfBytes);
      if (hasMainThumbnail) {
        safeProcessResource(
            entry -> processResourceWithThumbnail(entry, enrichedRdf, mediaExtractor,
                originSuccessExecutionRecordDTO.getDatasetId()),
            remainingResourcesList, warningExceptions);
      } else {
        safeProcessResource(
            entry -> processResourceWithoutThumbnail(entry, enrichedRdf, mediaExtractor,
                originSuccessExecutionRecordDTO.getDatasetId()), remainingResourcesList, warningExceptions);
      }

      final byte[] outputRdfBytes;
      outputRdfBytes = getOutputRdf(enrichedRdf);

      return createCopyIdentifiersValidated(originSuccessExecutionRecordDTO, targetExecutionId, getExecutionName(), b ->
          b.recordData(new String(outputRdfBytes, StandardCharsets.UTF_8))
           .exceptionWarnings(new HashSet<>(warningExceptions)));
    };
  }

  private void safeProcessResource(ThrowingFunction<RdfResourceEntry, Boolean> resourceProcessor, List<RdfResourceEntry> entries,
      List<Exception> recordProcessingExceptions) {
    for (RdfResourceEntry entry : entries) {
      try {
        resourceProcessor.apply(entry);
      } catch (Exception e) {
        recordProcessingExceptions.add(e);
      }
    }
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    return itemProcessorUtil.processCapturingException(originSuccessExecutionRecordDTO, targetExecutionId, getExecutionName());
  }

  private EnrichedRdf getEnrichedRdf(byte[] rdfBytes) throws RdfDeserializationException {
    return rdfDeserializer.getRdfForResourceEnriching(rdfBytes);
  }

  private byte[] getOutputRdf(EnrichedRdf rdfForEnrichment) throws RdfSerializationException {
    return rdfSerializer.serialize(rdfForEnrichment);
  }

  private boolean processResourceWithThumbnail(RdfResourceEntry resourceToProcess, EnrichedRdf rdfForEnrichment,
      MediaExtractor extractor, String datasetId) throws MediaExtractionException, ThumbnailStoringException {
    return processResource(resourceToProcess, rdfForEnrichment, extractor, true, datasetId);
  }

  private boolean processResourceWithoutThumbnail(RdfResourceEntry resourceToProcess, EnrichedRdf rdfForEnrichment,
      MediaExtractor extractor, String datasetId) throws MediaExtractionException, ThumbnailStoringException {
    return processResource(resourceToProcess, rdfForEnrichment, extractor, false, datasetId);
  }

  private boolean processResource(RdfResourceEntry resourceToProcess, EnrichedRdf rdfForEnrichment, MediaExtractor extractor,
      boolean gotMainThumbnail, String datasetId) throws MediaExtractionException, ThumbnailStoringException {
    final ResourceExtractionResult extraction = extractor.performMediaExtraction(resourceToProcess, gotMainThumbnail);

    // Check if extraction for media was successful
    boolean successful = (extraction != null);
    if (successful) {
      rdfForEnrichment.enrichResource(extraction.getMetadata());
      storeThumbnails(extraction.getThumbnails(), datasetId);
    }

    return successful;
  }

  private void storeThumbnails(List<Thumbnail> thumbnails, String datasetId) throws ThumbnailStoringException {
    if (isNotEmpty(thumbnails)) {
      thumbnailStoreService.store(thumbnails, datasetId);
    }
  }
}
