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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.function.ThrowingFunction;

/**
 * Service responsible for media metadata extraction.
 */
@AllArgsConstructor
@Service
public class MediaService {

  private final MediaExtractor mediaExtractor;
  private final ThumbnailStoreService thumbnailStoreService;
  private final RdfSerializer rdfSerializer;
  private final RdfDeserializer rdfDeserializer;

  /**
   * Extract media metadata from resources defined in the record and enriches the record with those while also creating thumbnails
   * where applicable.
   *
   * @param recordData the RDF data of the record to be processed
   * @param datasetId the identifier of the dataset to which the record belongs
   * @return a MediaProcessingResult containing the updated record data and any warning exceptions
   * @throws MediaExtractionException if there are issues during RDF processing or media extraction
   */
  public MediaProcessingResult processMediaRecord(String recordData, String datasetId) throws MediaExtractionException {
    byte[] rdfBytes = recordData.getBytes(StandardCharsets.UTF_8);
    EnrichedRdf enrichedRdf = getEnrichedRdf(rdfBytes);

    List<Exception> warningExceptions = new ArrayList<>();

    // Main thumbnail first
    RdfResourceEntry resourceMainThumbnail = getMainThumbnailResourceForMediaExtraction(rdfBytes);
    boolean hasMainThumbnail = false;
    if (resourceMainThumbnail != null) {
      safeProcessResource(
          entry -> processResourceWithoutThumbnail(entry, enrichedRdf, datasetId),
          List.of(resourceMainThumbnail),
          warningExceptions);
      hasMainThumbnail = warningExceptions.isEmpty();
    }

    // Remaining resources
    List<RdfResourceEntry> remainingResources = getRemainingResourcesForMediaExtraction(rdfBytes);
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
  }

  private List<RdfResourceEntry> getRemainingResourcesForMediaExtraction(byte[] rdfBytes) throws MediaExtractionException {
    List<RdfResourceEntry> remainingResources;
    try {
      remainingResources = rdfDeserializer.getRemainingResourcesForMediaExtraction(rdfBytes);
    } catch (RdfDeserializationException e) {
      throw new MediaExtractionException("Failed deserialization", e);
    }
    return remainingResources;
  }

  private RdfResourceEntry getMainThumbnailResourceForMediaExtraction(byte[] rdfBytes) throws MediaExtractionException {
    RdfResourceEntry resourceMainThumbnail;
    try {
      resourceMainThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(rdfBytes);
    } catch (RdfDeserializationException e) {
      throw new MediaExtractionException("Failed deserialization", e);
    }
    return resourceMainThumbnail;
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

  private EnrichedRdf getEnrichedRdf(byte[] rdfBytes) throws MediaExtractionException {
    try {
      return rdfDeserializer.getRdfForResourceEnriching(rdfBytes);
    } catch (RdfDeserializationException e) {
      throw new MediaExtractionException("Failed deserialization", e);
    }
  }

  private byte[] getOutputRdf(EnrichedRdf rdfForEnrichment) throws MediaExtractionException {
    try {
      return rdfSerializer.serialize(rdfForEnrichment);
    } catch (RdfSerializationException e) {
      throw new MediaExtractionException("Failed serialization", e);
    }
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

  /**
   * Represents the result of a media processing operation, including updated record data and warnings.
   *
   * @param updatedRecordData the updated RDF data after media processing
   * @param warningExceptions a list of warnings (exceptions) encountered during processing
   */
  public record MediaProcessingResult(String updatedRecordData, List<Exception> warningExceptions) {

  }
}

