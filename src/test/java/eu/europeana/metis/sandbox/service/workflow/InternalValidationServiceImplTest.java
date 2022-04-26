package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalValidationServiceImplTest {

  private static final String SCHEMA = "EDM-INTERNAL";

  @Mock
  private ValidationExecutionService validationExecutionService;

  @Mock
  private PatternAnalysisService<Step> patternAnalysisService;

  @InjectMocks
  private InternalValidationServiceImpl service;

  @Test
  void validate_expectSuccess() throws PatternAnalysisException {
    Map<String,LocalDateTime> mapping = spy(new HashMap<>());
    service.setMapping(mapping);
    var record = Record.builder().recordId(1L)
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(true);
    validationResult.setRecordId("1");

    when(validationExecutionService
        .singleValidation(eq(SCHEMA), isNull(), isNull(), any(InputStream.class)))
        .thenReturn(validationResult);
    doNothing().when(patternAnalysisService).generateRecordPatternAnalysis(eq("1"), eq(Step.VALIDATE_INTERNAL), any(LocalDateTime.class), anyString());

    var result = service.validate(record);

    assertEquals(record, result.getRecord());
    verify(mapping, times(1)).containsKey(anyString());
    verify(mapping, times(1)).put(anyString(), any(LocalDateTime.class));
  }

  @Test
  void validate_withTwoRecord_expectSuccess() throws PatternAnalysisException {
    Map<String,LocalDateTime> mapping = spy(new HashMap<>());
    service.setMapping(mapping);
    var record1 = Record.builder().recordId(1L)
            .content("".getBytes()).language(Language.IT).country(Country.ITALY)
            .datasetName("").datasetId("1").build();
    var record2 = Record.builder().recordId(2L)
            .content("".getBytes()).language(Language.IT).country(Country.ITALY)
            .datasetName("").datasetId("1").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(true);
    validationResult.setRecordId("1");

    when(validationExecutionService
            .singleValidation(eq(SCHEMA), isNull(), isNull(), any(InputStream.class)))
            .thenReturn(validationResult);
    doNothing().when(patternAnalysisService).generateRecordPatternAnalysis(eq("1"), eq(Step.VALIDATE_INTERNAL), any(LocalDateTime.class), anyString());

    var result1 = service.validate(record1);

    assertEquals(record1, result1.getRecord());

    var result2 = service.validate(record2);
    assertEquals(record2, result2.getRecord());
    verify(mapping, times(2)).containsKey("1");
    verify(mapping, times(1)).put(eq("1"), any(LocalDateTime.class));
    verify(mapping, times(1)).get("1");
  }

  @Test
  void validate_validationError_expectFail() {
    var record = Record.builder().recordId(1L)
        .content("".getBytes()).language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("1").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(false);
    validationResult.setRecordId("1");

    when(validationExecutionService
        .singleValidation(eq(SCHEMA), isNull(), isNull(), any(InputStream.class)))
        .thenReturn(validationResult);

    assertThrows(RecordValidationException.class, () -> service.validate(record));
  }

  @Test
  void validate_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.validate(null));
  }

  @Test
  void cleanMappingExecutionTimestamp_expectSuccess(){
    Map<String,LocalDateTime> mapping = new HashMap<>();
    mapping.put("1",LocalDateTime.now());
    service.setMapping(mapping);
    Map<String,LocalDateTime> result = service.cleanMappingExecutionTimestamp();
    assertEquals(0, result.size());
  }
}
