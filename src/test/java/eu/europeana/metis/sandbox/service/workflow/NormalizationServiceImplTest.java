package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationReport;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.normalization.util.NormalizationConfigurationException;
import eu.europeana.normalization.util.NormalizationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalizationServiceImplTest {

  @Mock
  private NormalizerFactory normalizerFactory;

  @Mock
  private Normalizer normalizer;

  @InjectMocks
  private NormalizationServiceImpl service;

  @Test
  void normalize_expectSuccess() throws NormalizationConfigurationException, NormalizationException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var normalizationResult = NormalizationResult.createInstanceForSuccess("success", new NormalizationReport());

    when(normalizerFactory.getNormalizer()).thenReturn(normalizer);
    when(normalizer.normalize(anyString())).thenReturn(normalizationResult);

    var result = service.normalize(record);

    assertEquals("success", result.getRecord().getContentString());
  }

  @Test
  void normalize_normalizationConfigException_expectFail()
      throws NormalizationConfigurationException, NormalizationException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    when(normalizerFactory.getNormalizer()).thenThrow(new NormalizationConfigurationException("issue", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.normalize(record));

    verify(normalizer, never()).normalize(anyString());
  }

  @Test
  void normalize_normalizationException_expectFail()
      throws NormalizationConfigurationException, NormalizationException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    when(normalizerFactory.getNormalizer()).thenReturn(normalizer);
    when(normalizer.normalize(anyString())).thenThrow(new NormalizationException("issue", new Exception()));

    assertThrows(RecordProcessingException.class, () -> service.normalize(record));
  }

  @Test
  void normalize_resultWithErrorMessage_expectFail()
      throws NormalizationConfigurationException, NormalizationException {
    var record = Record.builder().recordId("1")
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var normalizationResult = NormalizationResult.createInstanceForError("error", "fail");

    when(normalizerFactory.getNormalizer()).thenReturn(normalizer);
    when(normalizer.normalize(anyString())).thenReturn(normalizationResult);

    assertThrows(RecordProcessingException.class, () -> service.normalize(record));
  }

  @Test
  void normalize_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.normalize(null));
  }
}