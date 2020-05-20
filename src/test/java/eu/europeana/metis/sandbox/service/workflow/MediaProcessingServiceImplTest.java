package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import eu.europeana.metis.mediaprocessing.model.ResourceMetadata;
import eu.europeana.metis.mediaprocessing.model.Thumbnail;
import eu.europeana.metis.mediaprocessing.model.UrlType;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.ThumbnailStoringException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.util.ThumbnailService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaProcessingServiceImplTest {

  @Mock
  private RdfConverterFactory converterFactory;

  @Mock
  private MediaProcessorFactory processorFactory;

  @Mock
  private ThumbnailService thumbnailService;

  @Mock
  private RdfDeserializer deserializer;

  @Mock
  private RdfSerializer serializer;

  @Mock
  private MediaExtractor extractor;

  @Mock
  private EnrichedRdf enrichedRdf;

  @InjectMocks
  private MediaProcessingServiceImpl service;

  @Test
  void processMedia_expectSuccess() throws RdfConverterException, RdfDeserializationException,
      MediaProcessorException, MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn("This is new content".getBytes());
    when(extraction1.getMetadata()).thenReturn(metadata);
    when(extraction2.getMetadata()).thenReturn(metadata);

    var result = service.processMedia(record);

    assertArrayEquals("This is new content".getBytes(), result.getRecord().getContent());

    verify(extractor, times(2)).performMediaExtraction(any(RdfResourceEntry.class));
    verify(extraction1, times(1)).getThumbnails();
    verify(extraction2, times(1)).getThumbnails();
    verify(deserializer).getRdfForResourceEnriching(content);
    verify(enrichedRdf, times(2)).enrichResource(any(ResourceMetadata.class));
    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_createRdfDeserializer_expectFail() throws RdfConverterException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    when(converterFactory.createRdfDeserializer())
        .thenThrow(new RdfConverterException("failed", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(converterFactory).createRdfDeserializer();
    verifyNoMoreInteractions(converterFactory);
  }

  @Test
  void processMedia_createRdfSerializer_expectFail()
      throws RdfConverterException, MediaProcessorException,
      MediaExtractionException, RdfDeserializationException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer())
        .thenThrow(new RdfConverterException("", new Exception()));
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(extraction1.getMetadata()).thenReturn(metadata);
    when(extraction2.getMetadata()).thenReturn(metadata);

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(converterFactory).createRdfSerializer();
    verifyNoMoreInteractions(converterFactory);
  }

  @Test
  void processMedia_getRdfResourceEntries_expectFail()
      throws RdfConverterException, RdfDeserializationException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenThrow(new RdfDeserializationException("failed", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(deserializer).getResourceEntriesForMediaExtraction(content);
  }

  @Test
  void processMedia_getResourceExtractionResults_MediaProcessorException_expectFail()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenThrow(new MediaProcessorException(""));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(processorFactory).createMediaExtractor();
  }

  @Test
  void processMedia_getResourceExtractionResults_MediaExtractionException_expectSuccess()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException, IOException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenThrow(new MediaExtractionException(""));
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction);
    when(extraction.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn("This is new content".getBytes());
    when(extraction.getMetadata()).thenReturn(metadata);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);

    var result = service.processMedia(record);

    assertArrayEquals("This is new content".getBytes(), result.getRecord().getContent());
    assertFalse(result.getErrors().isEmpty());

    verify(extractor, times(2)).performMediaExtraction(any(RdfResourceEntry.class));
    verify(extraction, times(1)).getThumbnails();
    verify(deserializer).getRdfForResourceEnriching(content);
    verify(enrichedRdf, times(1)).enrichResource(any(ResourceMetadata.class));
    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_getResourceExtractionResults_returnNull_expectSuccess()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(any(RdfResourceEntry.class))).thenReturn(null);
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn(content);

    var result = service.processMedia(record);

    assertArrayEquals(content, result.getRecord().getContent());

    verifyNoMoreInteractions(thumbnailService);
    verifyNoMoreInteractions(enrichedRdf);
  }

  @Test
  void processMedia_storeThumbnails_expectSuccess()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(extraction2.getThumbnails()).thenReturn(List.of(thumbnail));
    doThrow(new ThumbnailStoringException("", new Exception())).when(thumbnailService)
        .store(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn("This is new content".getBytes());
    when(extraction1.getMetadata()).thenReturn(metadata);
    when(extraction2.getMetadata()).thenReturn(metadata);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);

    var result = service.processMedia(record);

    assertArrayEquals("This is new content".getBytes(), result.getRecord().getContent());
    assertFalse(result.getErrors().isEmpty());

    verify(extractor, times(2)).performMediaExtraction(any(RdfResourceEntry.class));
    verify(extraction1, times(1)).getThumbnails();
    verify(extraction2, times(1)).getThumbnails();
    verify(deserializer).getRdfForResourceEnriching(content);
    verify(enrichedRdf, times(2)).enrichResource(any(ResourceMetadata.class));
    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_getEnrichedRdf_RdfDeserializationException_expectFail()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(deserializer.getRdfForResourceEnriching(content))
        .thenThrow(new RdfDeserializationException("", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(deserializer).getRdfForResourceEnriching(content);
  }

  @Test
  void processMedia_getOutputRdf_RdfSerializationException_expectFail()
      throws RdfConverterException, RdfDeserializationException,
      MediaProcessorException, MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf))
        .thenThrow(new RdfSerializationException("", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.processMedia(null));
  }
}