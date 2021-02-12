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
import java.util.ArrayList;
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

    // Get resource entries
    var resourceEntries = getRdfResourceEntries(record, inputRdf, rdfDeserializer);

    var rdfForEnrichment = getEnrichedRdf(record, inputRdf, rdfDeserializer);

    List<RecordError> recordErrors = new LinkedList<>();
    try (MediaExtractor extractor = processorFactory.createMediaExtractor()) {
      for (RdfResourceEntry entry : resourceEntries) {
        processResourceEntry(record, rdfForEnrichment, extractor, entry, recordErrors);
      }
    } catch (MediaProcessorException | IOException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    // Get output rdf bytes
    var rdfSerializer = converterFactory.createRdfSerializer();
    byte[] outputRdf = getOutputRdf(record, rdfSerializer, rdfForEnrichment);

    return new RecordInfo(Record.from(record, outputRdf), recordErrors);
  }

  private void processResourceEntry(Record record, EnrichedRdf rdfForEnrichment,
      MediaExtractor extractor,
      RdfResourceEntry entry, List<RecordError> recordErrors) {
    try (ResourceExtractionResult extraction = extractor.performMediaExtraction(entry)) {
      if (nonNull(extraction)) {
        // Store thumbnails
        storeThumbnails(record, extraction.getThumbnails(), recordErrors);
        // Add result to RDF
        rdfForEnrichment.enrichResource(extraction.getMetadata());
      }
    } catch (MediaExtractionException | IOException e) {
      LOGGER.warn("Error while extracting media for record {}. ", record.getRecordId(), e);
      // collect warn
      recordErrors.add(new RecordError(new RecordProcessingException(record.getRecordId(), e)));
    }
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

  private List<RdfResourceEntry> getRdfResourceEntries(Record record, byte[] content,
      RdfDeserializer rdfDeserializer) {
    try {
      final List<RdfResourceEntry> result = new ArrayList<>(
          rdfDeserializer.getRemainingResourcesForMediaExtraction(content));
      final RdfResourceEntry mainThumbnailResource =
          rdfDeserializer.getMainThumbnailResourceForMediaExtraction(content);
      if (mainThumbnailResource != null) {
        result.add(mainThumbnailResource);
      }
      return result;
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
