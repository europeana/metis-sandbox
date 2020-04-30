package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.projection.ErrorLogView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetReportServiceImplTest {

  @Mock
  private RecordErrorLogRepository errorLogRepository;

  @InjectMocks
  private DatasetReportServiceImpl service;

  @Test
  void getReport_expectSuccess() {
    var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
    var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
    var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("1", "2"));
    var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("3", "4"));
    var errors = List.of(error1, error2);
    var reportByStep = new ReportByStepDto(Step.VALIDATE_EXTERNAL, errors);
    var report = new DatasetInfoDto("TBD", List.of(reportByStep));

    var errorView1 = new ErrorLogViewImpl(1L, "1", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
    var errorView2 = new ErrorLogViewImpl(1L, "2", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
    var errorView3 = new ErrorLogViewImpl(1L, "3", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
    var errorView4 = new ErrorLogViewImpl(1L, "4", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
    when(errorLogRepository.getByDatasetId(1))
        .thenReturn(List.of(errorView1, errorView2, errorView3, errorView4));

    var result = service.getReport(1);

    assertReportEquals(report, result);
  }

  @Test
  void getReport_emptyReport_expectSuccess() {
    when(errorLogRepository.getByDatasetId(1)).thenReturn(List.of());

    var result = service.getReport(1);

    assertTrue(result.getErrorsReport().isEmpty());
  }

  @Test
  void getReport_failToRetrieveData_expectFail() {
    when(errorLogRepository.getByDatasetId(1))
        .thenThrow(new ServiceException("failed", new Exception()));

    assertThrows(ServiceException.class, () -> service.getReport(1));
  }

  @Test
  void getReport_nullDatasetId_expectFail() {
    assertThrows(NullPointerException.class, () -> service.getReport(null));
  }

  private void assertReportEquals(DatasetInfoDto expected, DatasetInfoDto actual) {
    var errorsReportFromExpected = expected.getErrorsReport();
    var errorsReportFromActual = actual.getErrorsReport();
    assertEquals(errorsReportFromExpected.size(), errorsReportFromActual.size());
    for (int i = 0; i < errorsReportFromExpected.size(); i++) {
      assertEquals(errorsReportFromExpected.get(i).getStep(),
          errorsReportFromActual.get(i).getStep());
      var errorsByStepExpected = errorsReportFromExpected.get(i).getErrors();
      var errorsByStepActual = errorsReportFromActual.get(i).getErrors();
      assertEquals(errorsByStepExpected.size(), errorsByStepActual.size());
      for (int j = 0; j < errorsByStepExpected.size(); j++) {
        var recordIdsExpected = errorsByStepExpected.get(i).getRecordIds();
        var recordIdsActual = errorsByStepActual.get(i).getRecordIds();
        assertLinesMatch(recordIdsExpected, recordIdsActual);
      }
    }
  }

  private static class ErrorLogViewImpl implements ErrorLogView {

    private final Long id;
    private final String recordId;
    private final Integer datasetId;
    private final Step step;
    private final Status status;
    private final String message;

    public ErrorLogViewImpl(Long id, String recordId, Integer datasetId,
        Step step, Status status, String message) {
      this.id = id;
      this.recordId = recordId;
      this.datasetId = datasetId;
      this.step = step;
      this.status = status;
      this.message = message;
    }

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public String getRecordId() {
      return recordId;
    }

    @Override
    public Integer getDatasetId() {
      return datasetId;
    }

    @Override
    public Step getStep() {
      return step;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    @Override
    public String getMessage() {
      return message;
    }
  }
}