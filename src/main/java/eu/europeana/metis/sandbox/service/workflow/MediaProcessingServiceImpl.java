package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

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
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
class MediaProcessingServiceImpl implements MediaProcessingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessingServiceImpl.class);

  private final ThumbnailStoreService thumbnailStoreService;
  private final MediaExtractor mediaExtractor;
  private final RdfSerializer rdfSerializer;
  private final RdfDeserializer rdfDeserializer;

  public MediaProcessingServiceImpl(
      ThumbnailStoreService thumbnailStoreService,
      RdfSerializer rdfSerializer,
      RdfDeserializer rdfDeserializer,
      MediaExtractor mediaExtractor) {
    this.thumbnailStoreService = thumbnailStoreService;
    this.rdfSerializer = rdfSerializer;
    this.rdfDeserializer = rdfDeserializer;
    this.mediaExtractor = mediaExtractor;
  }

  @Override
  public RecordInfo processMedia(Record recordMedia) {
    requireNonNull(recordMedia, "Record must not be null");

    final byte[] inputRdf = recordMedia.getContent();

    List<RecordError> recordErrors = new LinkedList<>();
    EnrichedRdf rdfForEnrichment = getEnrichedRdf(recordMedia, inputRdf, rdfDeserializer);

    try {
      RdfResourceEntry resourceMainThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(inputRdf);
      boolean hasMainThumbnail = false;
      if (resourceMainThumbnail != null) {
        hasMainThumbnail = processResourceWithoutThumbnail(resourceMainThumbnail, recordMedia, rdfForEnrichment,
            mediaExtractor, recordErrors);
      }

      List<RdfResourceEntry> remainingResourcesList = rdfDeserializer.getRemainingResourcesForMediaExtraction(inputRdf);
      if (hasMainThumbnail) {
        remainingResourcesList.forEach(entry ->
            processResourceWithThumbnail(entry, recordMedia, rdfForEnrichment, mediaExtractor, recordErrors)
        );
      } else {
        remainingResourcesList.forEach(entry ->
            processResourceWithoutThumbnail(entry, recordMedia, rdfForEnrichment, mediaExtractor, recordErrors)
        );
      }

    } catch (RdfDeserializationException rdfDeserializationException) {
      LOGGER.warn("Error while extracting media for recordMedia {}. ", recordMedia.getProviderId(), rdfDeserializationException);
      throw new RecordProcessingException(recordMedia.getProviderId(), rdfDeserializationException);
    }

    // Get output rdf bytes
    final byte[] outputRdf = getOutputRdf(recordMedia, rdfSerializer, rdfForEnrichment);

    return new RecordInfo(Record.from(recordMedia, outputRdf), recordErrors);
  }

  private boolean processResourceWithThumbnail(RdfResourceEntry resourceToProcess, Record recordResource,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor, List<RecordError> recordErrors) {
    return processResource(resourceToProcess, recordResource, rdfForEnrichment, extractor, recordErrors, true);
  }

  private boolean processResourceWithoutThumbnail(RdfResourceEntry resourceToProcess, Record recordResource,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor, List<RecordError> recordErrors) {
    return processResource(resourceToProcess, recordResource, rdfForEnrichment, extractor, recordErrors, false);
  }

  /**
   * Processes the resource
   *
   * @return True if the media extraction for the resource was successful; False otherwise.
   */
  private boolean processResource(RdfResourceEntry resourceToProcess, Record recordResource,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor, List<RecordError> recordErrors,
      boolean gotMainThumbnail) {

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
          storeThumbnails(recordResource, extraction.getThumbnails(), recordErrors);
        }
      }

    } catch (MediaExtractionException e) {
      LOGGER.warn("Error while extracting media for record {}. ", recordResource.getProviderId(), e);
      // collect warnings
      recordErrors.add(new RecordError(new RecordProcessingException(recordResource.getProviderId(), e)));
    }

    return successful;
  }

  private void storeThumbnails(Record recordThumbnail, List<Thumbnail> thumbnails,
      List<RecordError> recordErrors) {
    if (nonNull(thumbnails)) {
      try {
        thumbnailStoreService.store(thumbnails, recordThumbnail.getDatasetId());
      } catch (ThumbnailStoringException e) {
        LOGGER.warn("Error while storing thumbnail for record {}. ", recordThumbnail.getProviderId(), e);
        // collect warn
        recordErrors.add(new RecordError(new RecordProcessingException(recordThumbnail.getProviderId(), e)));
      }
    }
  }

  private EnrichedRdf getEnrichedRdf(Record recordEnriched, byte[] inputRdf,
      RdfDeserializer rdfDeserializer) {
    try {
      return rdfDeserializer.getRdfForResourceEnriching(inputRdf);
    } catch (RdfDeserializationException e) {
      throw new RecordProcessingException(recordEnriched.getProviderId(), e);
    }
  }

  private byte[] getOutputRdf(Record recordOutput, RdfSerializer rdfSerializer,
      EnrichedRdf rdfForEnrichment) {
    try {
      return rdfSerializer.serialize(rdfForEnrichment);
    } catch (RdfSerializationException e) {
      throw new RecordProcessingException(recordOutput.getProviderId(), e);
    }
  }
}
