package eu.europeana.metis.sandbox.service.workflow;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

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
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.function.ThrowingFunction;

@Service
public class MediaService {

  private final MediaExtractor mediaExtractor;
  private final ThumbnailStoreService thumbnailStoreService;
  private final RdfSerializer rdfSerializer;
  private final RdfDeserializer rdfDeserializer;

  public MediaService(MediaExtractor mediaExtractor,
      ThumbnailStoreService thumbnailStoreService,
      RdfSerializer rdfSerializer,
      RdfDeserializer rdfDeserializer) {
    this.mediaExtractor = mediaExtractor;
    this.thumbnailStoreService = thumbnailStoreService;
    this.rdfSerializer = rdfSerializer;
    this.rdfDeserializer = rdfDeserializer;
  }

  public MediaProcessingResult processMediaRecord(String recordData, String datasetId) {
    try {
      byte[] rdfBytes = recordData.getBytes(StandardCharsets.UTF_8);
      EnrichedRdf enrichedRdf = getEnrichedRdf(rdfBytes);

      List<Exception> warningExceptions = new ArrayList<>();

      // Main thumbnail first
      RdfResourceEntry resourceMainThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(rdfBytes);
      boolean hasMainThumbnail = false;
      if (resourceMainThumbnail != null) {
        safeProcessResource(
            entry -> processResourceWithoutThumbnail(entry, enrichedRdf, datasetId),
            List.of(resourceMainThumbnail),
            warningExceptions);
        hasMainThumbnail = warningExceptions.isEmpty();
      }

      // Remaining resources
      List<RdfResourceEntry> remainingResources = rdfDeserializer.getRemainingResourcesForMediaExtraction(rdfBytes);
      if (hasMainThumbnail) {
        safeProcessResource(
            entry -> processResourceWithThumbnail(entry, enrichedRdf, datasetId),
            remainingResources,
            warningExceptions);
      } else {
        safeProcessResource(
            entry -> processResourceWithoutThumbnail(entry, enrichedRdf, datasetId),
            remainingResources,
            warningExceptions);
      }

      // Serialize RDF
      byte[] outputRdfBytes = getOutputRdf(enrichedRdf);
      String updatedRecordData = new String(outputRdfBytes, StandardCharsets.UTF_8);

      return new MediaProcessingResult(updatedRecordData, warningExceptions);

    } catch (Exception e) {
      throw new RuntimeException("Media processing failed", e);
    }
  }

  private void safeProcessResource(ThrowingFunction<RdfResourceEntry, Boolean> resourceProcessor,
      List<RdfResourceEntry> entries, List<Exception> recordProcessingExceptions) {
    for (RdfResourceEntry entry : entries) {
      try {
        resourceProcessor.apply(entry);
      } catch (Exception e) {
        recordProcessingExceptions.add(e);
      }
    }
  }

  private EnrichedRdf getEnrichedRdf(byte[] rdfBytes) throws RdfDeserializationException {
    return rdfDeserializer.getRdfForResourceEnriching(rdfBytes);
  }

  private byte[] getOutputRdf(EnrichedRdf rdfForEnrichment) throws RdfSerializationException {
    return rdfSerializer.serialize(rdfForEnrichment);
  }

  private boolean processResourceWithThumbnail(RdfResourceEntry resourceToProcess, EnrichedRdf rdfForEnrichment,
      String datasetId) throws MediaExtractionException, ThumbnailStoringException {
    return processResource(resourceToProcess, rdfForEnrichment, true, datasetId);
  }

  private boolean processResourceWithoutThumbnail(RdfResourceEntry resourceToProcess, EnrichedRdf rdfForEnrichment,
      String datasetId) throws MediaExtractionException, ThumbnailStoringException {
    return processResource(resourceToProcess, rdfForEnrichment, false, datasetId);
  }

  private boolean processResource(RdfResourceEntry resourceToProcess, EnrichedRdf rdfForEnrichment,
      boolean gotMainThumbnail, String datasetId) throws MediaExtractionException, ThumbnailStoringException {
    ResourceExtractionResult extraction = mediaExtractor.performMediaExtraction(resourceToProcess, gotMainThumbnail);

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

  public record MediaProcessingResult(String updatedRecordData, List<Exception> warningExceptions) {}
}

