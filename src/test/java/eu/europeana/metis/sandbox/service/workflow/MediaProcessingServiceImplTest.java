package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import eu.europeana.metis.sandbox.service.util.ThumbnailStoreService;
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
  private ThumbnailStoreService thumbnailStoreService;

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
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content.getBytes())).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn("This is new content".getBytes());
    when(extraction1.getMetadata()).thenReturn(metadata);
    when(extraction2.getMetadata()).thenReturn(metadata);

    Record result = service.processMedia(record);

    assertEquals("This is new content", result.getContent());

    verify(extractor, times(2)).performMediaExtraction(any(RdfResourceEntry.class));
    verify(extraction1, times(1)).getThumbnails();
    verify(extraction2, times(1)).getThumbnails();
    verify(deserializer).getRdfForResourceEnriching(content.getBytes());
    verify(enrichedRdf, times(2)).enrichResource(any(ResourceMetadata.class));
    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_createRdfDeserializer_expectFail() throws RdfConverterException {
    var record = Record.builder().recordId("1")
        .content("").language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    when(converterFactory.createRdfDeserializer())
        .thenThrow(new RdfConverterException("failed", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(converterFactory).createRdfDeserializer();
    verifyNoMoreInteractions(converterFactory);
  }

  @Test
  void processMedia_createRdfSerializer_expectFail()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer()).thenThrow(new RdfConverterException("", new Exception()));
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content.getBytes())).thenReturn(enrichedRdf);
    when(extraction1.getMetadata()).thenReturn(metadata);
    when(extraction2.getMetadata()).thenReturn(metadata);

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(converterFactory).createRdfSerializer();
    verifyNoMoreInteractions(converterFactory);
  }

  @Test
  void processMedia_getRdfResourceEntries_expectFail()
      throws RdfConverterException, RdfDeserializationException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenThrow(new RdfDeserializationException("failed", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(deserializer).getResourceEntriesForMediaExtraction(content.getBytes());
  }

  @Test
  void processMedia_getResourceExtractionResults_MediaProcessorException_expectFail()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenThrow(new MediaProcessorException(""));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(processorFactory).createMediaExtractor();
  }

  @Test
  void processMedia_getResourceExtractionResults_MediaExtractionException_expectFail()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException, IOException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenThrow(new MediaExtractionException(""));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(extractor).performMediaExtraction(entry1);
    verify(extractor).close();
    verifyNoMoreInteractions(extractor);
  }

  @Test
  void processMedia_getResourceExtractionResults_returnNull_expectSuccess()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException, RdfSerializationException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(any(RdfResourceEntry.class))).thenReturn(null);
    when(deserializer.getRdfForResourceEnriching(content.getBytes())).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn(content.getBytes());

    Record result = service.processMedia(record);

    assertEquals(content, result.getContent());
    verify(thumbnailStoreService).store(List.of());
  }

  @Test
  void processMedia_storeThumbnails_expectFail()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException, IOException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    doThrow(new ThumbnailStoringException("", new Exception())).when(thumbnailStoreService)
        .store(List.of(thumbnail));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(extractor, times(2)).performMediaExtraction(any(RdfResourceEntry.class));
    verify(thumbnailStoreService).store(List.of(thumbnail));
  }

  @Test
  void processMedia_getEnrichedRdf_RdfDeserializationException_expectFail()
      throws RdfConverterException, RdfDeserializationException, MediaProcessorException,
      MediaExtractionException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content.getBytes()))
        .thenThrow(new RdfDeserializationException("", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));

    verify(deserializer).getRdfForResourceEnriching(content.getBytes());
  }

  @Test
  void processMedia_getOutputRdf_RdfSerializationException_expectFail()
      throws RdfConverterException, RdfDeserializationException,
      MediaProcessorException, MediaExtractionException, RdfSerializationException {
    var content = "this is the content";
    var record = Record.builder().recordId("1")
        .content(content).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION));
    var resourceEntries = List.of(entry1, entry2);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);

    when(converterFactory.createRdfDeserializer()).thenReturn(deserializer);
    when(converterFactory.createRdfSerializer()).thenReturn(serializer);
    when(deserializer.getResourceEntriesForMediaExtraction(content.getBytes()))
        .thenReturn(resourceEntries);
    when(processorFactory.createMediaExtractor()).thenReturn(extractor);
    when(extractor.performMediaExtraction(entry1)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content.getBytes())).thenReturn(enrichedRdf);
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