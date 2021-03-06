package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.exceptions.DereferenceException;
import eu.europeana.enrichment.rest.client.exceptions.EnrichmentException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.schema.convert.SerializationException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceImplTest {

  @Mock
  private EnrichmentWorker enrichmentWorker;

  @InjectMocks
  private EnrichmentServiceImpl service;

  @Test
  void enrich_expectSuccess()
      throws EnrichmentException, DereferenceException, SerializationException {
    var content = "This is the content";
    var newContent = "This is new content".getBytes();
    var record = Record.builder().recordId("1")
        .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    when(enrichmentWorker.process(any(InputStream.class))).thenReturn(newContent);
    var result = service.enrich(record);

    assertArrayEquals(newContent, result.getRecord().getContent());
  }

  @Test
  void enrich_withDereferenceException_expectFail()
      throws EnrichmentException, DereferenceException, SerializationException {
    var content = "This is the content";
    var record = Record.builder().recordId("1")
        .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();
    when(enrichmentWorker.process(any(InputStream.class)))
        .thenThrow(new EnrichmentException("Failed", new Exception()));
    var recordInfo = service.enrich(record);

    assertEquals("1", recordInfo.getRecord().getRecordId());
    assertEquals(1, recordInfo.getErrors().size());
  }

  @Test
  void enrich_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.enrich(null));
  }
}
