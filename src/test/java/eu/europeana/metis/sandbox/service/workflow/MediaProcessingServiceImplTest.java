package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.mediaprocessing.MediaExtractor;
import eu.europeana.metis.mediaprocessing.RdfDeserializer;
import eu.europeana.metis.mediaprocessing.RdfSerializer;
import eu.europeana.metis.mediaprocessing.exception.MediaExtractionException;
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
  void processMedia_expectSuccess() throws RdfDeserializationException, MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var testRecord = Record.builder().recordId(1L)
                       .content(content).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(deserializer.getMainThumbnailResourceForMediaExtraction(content))
        .thenReturn(entry1);
    when(deserializer.getRemainingResourcesForMediaExtraction(content))
        .thenReturn(List.of(entry2));
    when(extractor.performMediaExtraction(entry1, false)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2, true)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(extraction1.getMetadata()).thenReturn(metadata);
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn("This is new content".getBytes());
    when(extraction2.getMetadata()).thenReturn(metadata);
    when(extraction2.getThumbnails()).thenReturn(List.of(thumbnail));

    var result = service.processMedia(testRecord);

    assertArrayEquals("This is new content".getBytes(), result.getRecordValue().getContent());

    verify(extractor, times(2)).performMediaExtraction(any(RdfResourceEntry.class), anyBoolean());
    verify(extraction1, times(2)).getThumbnails();
    verify(extraction1, times(1)).getMetadata();
    verify(extraction2, times(2)).getThumbnails();
    verify(extraction2, times(1)).getMetadata();
    verify(deserializer).getRdfForResourceEnriching(content);
    verify(enrichedRdf, times(2)).enrichResource(any(ResourceMetadata.class));
    verify(serializer).serialize(enrichedRdf);
  }


  @Test
  void processMedia_getResourceExtractionResults_MediaExtractionException_expectSuccess()
      throws RdfDeserializationException, MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var testRecord = Record.builder().recordId(1L)
                       .content(content).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);

    when(deserializer.getMainThumbnailResourceForMediaExtraction(content))
        .thenReturn(entry1);
    when(extractor.performMediaExtraction(entry1, false)).thenThrow(new MediaExtractionException(""));

    when(deserializer.getMainThumbnailResourceForMediaExtraction(content)).thenReturn(entry1);
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn("This is new content".getBytes());

    var result = service.processMedia(testRecord);

    assertArrayEquals("This is new content".getBytes(), result.getRecordValue().getContent());
    assertFalse(result.getErrors().isEmpty());

    verify(extractor, times(1)).performMediaExtraction(any(RdfResourceEntry.class), anyBoolean());
    verify(deserializer).getRdfForResourceEnriching(content);
    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_getResourceExtractionResults_returnNull_expectSuccess()
      throws RdfDeserializationException, MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var testRecord = Record.builder().recordId(1L)
                       .content(content).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);

    when(deserializer.getRemainingResourcesForMediaExtraction(content))
        .thenReturn(List.of(entry2));
    when(deserializer.getMainThumbnailResourceForMediaExtraction(content))
        .thenReturn(entry1);
    when(extractor.performMediaExtraction(any(RdfResourceEntry.class), anyBoolean())).thenReturn(null);
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn(content);

    var result = service.processMedia(testRecord);

    assertArrayEquals(content, result.getRecordValue().getContent());

    verifyNoMoreInteractions(thumbnailStoreService);
    verifyNoMoreInteractions(enrichedRdf);
  }

  @Test
  void processMedia_storeThumbnails_expectSuccess()
      throws RdfDeserializationException, MediaExtractionException, RdfSerializationException {
    var content = "this is the content".getBytes();
    var testRecord = Record.builder().recordId(1L)
                       .content(content).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);
    var metadata = mock(ResourceMetadata.class);

    when(deserializer.getRemainingResourcesForMediaExtraction(content))
        .thenReturn(List.of(entry2));
    when(deserializer.getMainThumbnailResourceForMediaExtraction(content))
        .thenReturn(entry1);
    when(extractor.performMediaExtraction(entry1, false)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2, true)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(extraction2.getThumbnails()).thenReturn(List.of(thumbnail));
    when(extraction1.getMetadata()).thenReturn(metadata);
    when(extraction2.getMetadata()).thenReturn(metadata);
    doThrow(new ThumbnailStoringException("", new Exception())).when(thumbnailStoreService)
                                                               .store(List.of(thumbnail), "1");
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf)).thenReturn("This is new content".getBytes());

    var result = service.processMedia(testRecord);

    assertArrayEquals("This is new content".getBytes(), result.getRecordValue().getContent());
    assertFalse(result.getErrors().isEmpty());

    verify(extractor, times(2)).performMediaExtraction(any(RdfResourceEntry.class), anyBoolean());
    verify(extraction1, times(2)).getThumbnails();
    verify(deserializer).getRdfForResourceEnriching(content);
    verify(enrichedRdf, times(2)).enrichResource(any(ResourceMetadata.class));
    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_getEnrichedRdf_RdfDeserializationException_expectFail()
      throws RdfDeserializationException {
    var content = "this is the content".getBytes();
    var testRecord = Record.builder().recordId(1L)
                       .content(content).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    when(deserializer.getRdfForResourceEnriching(content))
        .thenThrow(new RdfDeserializationException("", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(testRecord));

    verify(deserializer).getRdfForResourceEnriching(content);
  }

  @Test
  void processMedia_getOutputRdf_RdfSerializationException_expectFail()
      throws RdfDeserializationException, MediaExtractionException,
      RdfSerializationException {
    var content = "this is the content".getBytes();
    var testRecord = Record.builder().recordId(1L)
                       .content(content).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var entry1 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);
    var entry2 = new RdfResourceEntry("", new ArrayList<>(UrlType.URL_TYPES_FOR_MEDIA_EXTRACTION), false);
    var extraction1 = mock(ResourceExtractionResult.class);
    var extraction2 = mock(ResourceExtractionResult.class);
    var thumbnail = mock(Thumbnail.class);

    when(deserializer.getMainThumbnailResourceForMediaExtraction(content))
        .thenReturn(entry1);
    when(deserializer.getRemainingResourcesForMediaExtraction(content))
        .thenReturn(List.of(entry2));
    when(extractor.performMediaExtraction(entry1, false)).thenReturn(extraction1);
    when(extractor.performMediaExtraction(entry2, true)).thenReturn(extraction2);
    when(extraction1.getThumbnails()).thenReturn(List.of(thumbnail));
    when(deserializer.getRdfForResourceEnriching(content)).thenReturn(enrichedRdf);
    when(serializer.serialize(enrichedRdf))
        .thenThrow(new RdfSerializationException("", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(testRecord));

    verify(serializer).serialize(enrichedRdf);
  }

  @Test
  void processMedia_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.processMedia(null));
  }
}
