package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.RdfConverterFactory;
import eu.europeana.metis.mediaprocessing.RdfDeserializer;
import eu.europeana.metis.mediaprocessing.RdfSerializer;
import eu.europeana.metis.mediaprocessing.exception.MediaExtractionException;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mediaprocessing.exception.RdfConverterException;
import eu.europeana.metis.mediaprocessing.exception.RdfDeserializationException;
import eu.europeana.metis.mediaprocessing.exception.RdfSerializationException;
import eu.europeana.metis.mediaprocessing.model.EnrichedRdf;
import eu.europeana.metis.mediaprocessing.model.RdfResourceEntry;
import eu.europeana.metis.mediaprocessing.model.ResourceExtractionResult;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class MediaProcessingServiceImpl implements MediaProcessingService {

  private final RdfConverterFactory converterFactory;
  private final MediaProcessorFactory processorFactory;

  public MediaProcessingServiceImpl(
      RdfConverterFactory converterFactory,
      MediaProcessorFactory processorFactory) {
    this.converterFactory = converterFactory;
    this.processorFactory = processorFactory;
  }

  @Override
  public Record processMedia(Record record) {
    requireNonNull(record, "Record must not be null");

    RdfDeserializer rdfDeserializer;
    RdfSerializer rdfSerializer;
    try {
      rdfDeserializer = converterFactory.createRdfDeserializer();
      rdfSerializer = converterFactory.createRdfSerializer();
    } catch (RdfConverterException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    var inputRdf = record.getContentBytes();

    // Get resource entries
    var resourceEntries = getRdfResourceEntries(record, inputRdf,
        rdfDeserializer);

    // Perform metadata extraction
    var results = getResourceExtractionResults(record, resourceEntries);

    // Process thumbnails
    processThumbnails(record, results);

    // Add result to RDF
    var rdfForEnrichment = getEnrichedRdf(record, inputRdf, rdfDeserializer, results);

    byte[] outputRdf = getOutputRdf(record, rdfSerializer, rdfForEnrichment);

    return Record.from(record, outputRdf);
  }

  private List<RdfResourceEntry> getRdfResourceEntries(Record record, byte[] content,
      RdfDeserializer rdfDeserializer) {
    List<RdfResourceEntry> resourceEntries;
    try {
      resourceEntries = rdfDeserializer
          .getResourceEntriesForMediaExtraction(content);
    } catch (RdfDeserializationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }
    return resourceEntries;
  }

  private List<ResourceExtractionResult> getResourceExtractionResults(Record record,
      List<RdfResourceEntry> resourceEntries) {
    List<ResourceExtractionResult> results = new ArrayList<>();
    try (MediaExtractor extractor = processorFactory.createMediaExtractor()) {
      for (RdfResourceEntry entry : resourceEntries) {
        var extraction = extractor.performMediaExtraction(entry);
        if (extraction != null) {
          results.add(extraction);
        }
      }
    } catch (IOException | MediaProcessorException | MediaExtractionException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }
    return results;
  }

  private EnrichedRdf getEnrichedRdf(Record record, byte[] content,
      RdfDeserializer rdfDeserializer, List<ResourceExtractionResult> results) {
    EnrichedRdf rdfForEnrichment;
    try {
      rdfForEnrichment = rdfDeserializer.getRdfForResourceEnriching(content);
    } catch (RdfDeserializationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }
    results.stream().map(ResourceExtractionResult::getMetadata).filter(Objects::nonNull)
        .forEach(rdfForEnrichment::enrichResource);
    return rdfForEnrichment;
  }

  private void processThumbnails(Record record, List<ResourceExtractionResult> results) {
    for (ResourceExtractionResult result : results) {
      if (result.getThumbnails() != null) {
        for (Thumbnail thumbnail : result.getThumbnails()) {
          // TODO save thumbnail
          try {
            thumbnail.close();
          } catch (IOException e) {
            throw new RecordProcessingException(record.getRecordId(), e);
          }
        }
      }
    }
  }

  private byte[] getOutputRdf(Record record, RdfSerializer rdfSerializer,
      EnrichedRdf rdfForEnrichment) {
    byte[] outputRdf;
    try {
      outputRdf = rdfSerializer.serialize(rdfForEnrichment);
    } catch (RdfSerializationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }
    return outputRdf;
  }
}
