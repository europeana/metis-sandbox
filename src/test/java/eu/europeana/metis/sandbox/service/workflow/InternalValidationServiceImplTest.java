package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;

@ExtendWith(MockitoExtension.class)
class InternalValidationServiceImplTest {

  private static final String SCHEMA = "EDM-INTERNAL";

  @Mock
  private ValidationExecutionService validationExecutionService;

  @Mock
  private PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;

  @Mock
  private ExecutionPointService executionPointService;

  @Mock
  private LockRegistry lockRegistry;

  @InjectMocks
  private InternalValidationServiceImpl service;

  @Test
  void validate_expectSuccess() throws PatternAnalysisException {
    var testRecord = Record.builder().recordId(1L)
                       .content("".getBytes()).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(true);
    validationResult.setRecordId("1");

    when(validationExecutionService
        .singleValidation(eq(SCHEMA), isNull(), isNull(), any(InputStream.class)))
        .thenReturn(validationResult);
    when(executionPointService.getExecutionPoint("1", FullBatchJobType.VALIDATE_INTERNAL.toString())).thenReturn(Optional.empty());
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());
    when(patternAnalysisService.initializePatternAnalysisExecution(anyString(), any(FullBatchJobType.class),
        any(LocalDateTime.class))).thenReturn(new ExecutionPoint());
    doNothing().when(patternAnalysisService).generateRecordPatternAnalysis(any(ExecutionPoint.class), anyString());

    var result = service.validate(testRecord);

    assertEquals(testRecord, result.getRecordValue());
  }

  @Test
  void validate_validationError_expectFail() {
    var testRecord = Record.builder().recordId(1L)
                       .content("".getBytes()).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(false);
    validationResult.setRecordId("1");

    when(validationExecutionService
        .singleValidation(eq(SCHEMA), isNull(), isNull(), any(InputStream.class)))
        .thenReturn(validationResult);

    assertThrows(RecordValidationException.class, () -> service.validate(testRecord));
  }

  @Test
  void validate_pattern_analysis_fail_but_successful_return() throws PatternAnalysisException {
    var testRecord = Record.builder().recordId(1L)
                       .content("".getBytes()).language(Language.IT).country(Country.ITALY)
                       .datasetName("").datasetId("1").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(true);
    validationResult.setRecordId("1");

    when(validationExecutionService
        .singleValidation(eq(SCHEMA), isNull(), isNull(), any(InputStream.class)))
        .thenReturn(validationResult);
    when(executionPointService.getExecutionPoint("1", FullBatchJobType.VALIDATE_INTERNAL.toString())).thenReturn(Optional.empty());
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());
    when(patternAnalysisService.initializePatternAnalysisExecution(anyString(), any(FullBatchJobType.class),
        any(LocalDateTime.class))).thenReturn(new ExecutionPoint());
    doThrow(new PatternAnalysisException("Error", null)).when(patternAnalysisService)
                                                        .generateRecordPatternAnalysis(any(ExecutionPoint.class), anyString());

    var result = assertDoesNotThrow(() -> service.validate(testRecord));
    assertEquals(testRecord, result.getRecordValue());

  }

  @Test
  void validate_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.validate(null));
  }
}
