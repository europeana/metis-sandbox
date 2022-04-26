package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

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
}
