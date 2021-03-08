package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

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
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class MediaProcessingServiceImpl implements MediaProcessingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessingServiceImpl.class);

  private final RdfConverterFactory converterFactory;
  private final MediaProcessorFactory processorFactory;
  private final ThumbnailStoreService thumbnailStoreService;

  public MediaProcessingServiceImpl(
      RdfConverterFactory converterFactory,
      MediaProcessorFactory processorFactory,
      ThumbnailStoreService thumbnailStoreService) {
    this.converterFactory = converterFactory;
    this.processorFactory = processorFactory;
    this.thumbnailStoreService = thumbnailStoreService;
  }

  @Override
  public RecordInfo processMedia(Record record) {
    requireNonNull(record, "Record must not be null");

    var inputRdf = record.getContent();
    var rdfDeserializer = converterFactory.createRdfDeserializer();

    List<RecordError> recordErrors = new LinkedList<>();
    EnrichedRdf rdfForEnrichment = getEnrichedRdf(record, inputRdf, rdfDeserializer);

    try (MediaExtractor extractor = processorFactory.createMediaExtractor()) {
      // Get main thumbnail
      RdfResourceEntry resourceThumbnail = rdfDeserializer.getMainThumbnailResourceForMediaExtraction(inputRdf);
      boolean hasMainThumbnail = processResource(resourceThumbnail, record, rdfForEnrichment,
          extractor, recordErrors, false);

      // Process remaining resources
      List<RdfResourceEntry> remainingResources = rdfDeserializer.getRemainingResourcesForMediaExtraction(inputRdf);
      for (RdfResourceEntry entry : remainingResources) {
        processResource(entry, record, rdfForEnrichment, extractor, recordErrors, hasMainThumbnail);
      }

    } catch (MediaProcessorException | IOException | RdfDeserializationException e) {
      LOGGER.warn("Error while extracting media for record {}. ", record.getRecordId(), e);
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    // Get output rdf bytes
    var rdfSerializer = converterFactory.createRdfSerializer();
    byte[] outputRdf = getOutputRdf(record, rdfSerializer, rdfForEnrichment);

    return new RecordInfo(Record.from(record, outputRdf), recordErrors);
  }

  /**
   * Processes the resource
   *
   * @return True if the media extraction for the resource was successful; False otherwise.
   */
  private boolean processResource(RdfResourceEntry resourceToProcess, Record record,
      EnrichedRdf rdfForEnrichment, MediaExtractor extractor, List<RecordError> recordErrors,
      boolean gotMainThumbnail){

    ResourceExtractionResult extraction;
    boolean successful = false;

    try{
      // Perform media extraction
      extraction = extractor.performMediaExtraction(resourceToProcess, gotMainThumbnail);

      // Check if extraction for media was successful
      successful = extraction != null;

      // If successful then store data
      if(successful){
        rdfForEnrichment.enrichResource(extraction.getMetadata());
        List<Thumbnail> thumbnails = extraction.getThumbnails();
        if(thumbnails != null && !thumbnails.isEmpty()) {
          storeThumbnails(record, extraction.getThumbnails(), recordErrors);
        }
      }

    } catch(MediaExtractionException e) {
      LOGGER.warn("Error while extracting media for record {}. ", record.getRecordId(), e);
      // collect warnings
      recordErrors.add(new RecordError(new RecordProcessingException(record.getRecordId(), e)));
    }

    return successful;

  }

  private void storeThumbnails(Record record, List<Thumbnail> thumbnails,
      List<RecordError> recordErrors) {
    if (nonNull(thumbnails)) {
      try {
        thumbnailStoreService.store(thumbnails, record.getDatasetId());
      } catch (ThumbnailStoringException e) {
        LOGGER.warn("Error while storing thumbnail for record {}. ", record.getRecordId(), e);
        // collect warn
        recordErrors.add(new RecordError(new RecordProcessingException(record.getRecordId(), e)));
      }
    }
  }

  private EnrichedRdf getEnrichedRdf(Record record, byte[] inputRdf,
      RdfDeserializer rdfDeserializer) {
    try {
      return rdfDeserializer.getRdfForResourceEnriching(inputRdf);
    } catch (RdfDeserializationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }
  }

  private byte[] getOutputRdf(Record record, RdfSerializer rdfSerializer,
      EnrichedRdf rdfForEnrichment) {
    try {
      return rdfSerializer.serialize(rdfForEnrichment);
    } catch (RdfSerializationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }
  }
}
