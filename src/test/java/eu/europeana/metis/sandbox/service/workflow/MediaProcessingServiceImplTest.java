package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.RdfConverterFactory;
import eu.europeana.metis.mediaprocessing.exception.RdfConverterException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
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

  @InjectMocks
  private MediaProcessingServiceImpl service;

  @Test
  void processMedia_expectSuccess() {
  }

  @Test
  void processMedia_converterFactoryException_expectFail() throws RdfConverterException {
    var record = Record.builder().recordId("1")
        .content("").language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    when(converterFactory.createRdfDeserializer()).thenThrow(new RdfConverterException("failed", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.processMedia(record));
  }

  @Test
  void processMedia_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.processMedia(null));
  }
}