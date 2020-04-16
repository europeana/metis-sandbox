package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.validation.model.ValidationResult;
import eu.europeana.validation.service.ValidationExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalValidationServiceImplTest {

  private static final String SCHEMA = "EDM-EXTERNAL";

  @Mock
  private OrderingService orderingService;

  @Mock
  private ValidationExecutionService validationExecutionService;

  @InjectMocks
  private ExternalValidationServiceImpl service;

  @Test
  void validate_expectSuccess() throws TransformationException {
    var record = Record.builder().recordId("1")
        .content("").language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(true);
    validationResult.setRecordId("1");

    when(orderingService.performOrdering("".getBytes())).thenReturn("");
    when(validationExecutionService.singleValidation(SCHEMA, null, null, ""))
        .thenReturn(validationResult);

    var result = service.validate(record);

    assertEquals(record, result);
  }

  @Test
  void validate_orderingError_expectFail() throws TransformationException {
    var record = Record.builder().recordId("1")
        .content("").language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    when(orderingService.performOrdering("".getBytes()))
        .thenThrow(new TransformationException(new Exception("issue")));

    assertThrows(RecordProcessingException.class, () -> service.validate(record));

    verifyNoInteractions(validationExecutionService);
  }

  @Test
  void validate_validationError_expectFail() throws TransformationException {
    var record = Record.builder().recordId("1")
        .content("").language(Language.IT).country(Country.ITALY)
        .datasetName("").datasetId("").build();

    var validationResult = new ValidationResult();
    validationResult.setSuccess(false);
    validationResult.setRecordId("1");

    when(orderingService.performOrdering("".getBytes())).thenReturn("");
    when(validationExecutionService.singleValidation(SCHEMA, null, null, ""))
        .thenReturn(validationResult);

    assertThrows(RecordValidationException.class, () -> service.validate(record));
  }

  @Test
  void validate_inputNull_expectFail() {
    assertThrows(NullPointerException.class, () -> service.validate(null));
  }
}