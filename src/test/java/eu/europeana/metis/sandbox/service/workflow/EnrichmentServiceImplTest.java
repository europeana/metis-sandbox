package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import eu.europeana.enrichment.rest.client.DereferenceOrEnrichException;
import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.UnsupportedEncodingException;
import org.jibx.runtime.JiBXException;
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
      throws DereferenceOrEnrichException, UnsupportedEncodingException, JiBXException {
    var content = "This is the content";
    var newContent = "This is new content";
    var record = Record.builder().recordId("1")
        .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    when(enrichmentWorker.process(content)).thenReturn(newContent);
    var result = service.enrich(record);

    assertEquals(newContent, result.getRecord().getContentString());
  }

  @Test
  void enrich_withDereferenceException_expectFail()
      throws DereferenceOrEnrichException, UnsupportedEncodingException, JiBXException {
    var content = "This is the content";
    var record = Record.builder().recordId("1")
        .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();
    when(enrichmentWorker.process(content))
        .thenThrow(new DereferenceOrEnrichException("Failed", new Exception()));
    assertThrows(RecordProcessingException.class, () -> service.enrich(record));
  }

  @Test
  void enrich_withUnsupportedEncodingException_expectFail()
      throws DereferenceOrEnrichException, UnsupportedEncodingException, JiBXException {
    var content = "This is the content";
    var record = Record.builder().recordId("1")
        .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();
    when(enrichmentWorker.process(content)).thenThrow(new UnsupportedEncodingException());
    assertThrows(RecordProcessingException.class, () -> service.enrich(record));
  }

  @Test
  void enrich_withJiBXException_expectFail()
      throws DereferenceOrEnrichException, UnsupportedEncodingException, JiBXException {
    var content = "This is the content";
    var record = Record.builder().recordId("1")
        .content(content.getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();
    when(enrichmentWorker.process(content)).thenThrow(new JiBXException("Failed"));
    assertThrows(RecordProcessingException.class, () -> service.enrich(record));
  }

  @Test
  void enrich_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.enrich(null));
  }
}
