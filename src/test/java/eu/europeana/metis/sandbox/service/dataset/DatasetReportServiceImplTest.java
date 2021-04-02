package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.report.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogView;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetReportServiceImplTest {

  @Mock
  private DatasetRepository datasetRepository;

  @Mock
  private RecordLogRepository recordLogRepository;

  @Mock
  private RecordErrorLogRepository errorLogRepository;

  @InjectMocks
  private DatasetReportServiceImpl service;

  @BeforeEach
  void setup() {
    setField(service, "portalPreviewUrl",
        "https://metis-sandbox/portal/preview/search?q=edm_datasetName:");
    setField(service, "portalPublishUrl",
        "https://metis-sandbox/portal/publish/search?q=edm_datasetName:");
  }

  @Test
  void getReportWithErrors_expectSuccess() {
    var dataset = new DatasetEntity("dataset", 5, Language.NL, Country.NETHERLANDS);
    var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
    var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
    var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("1", "2"));
    var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("3", "4"));
    var errors = List.of(error1, error2);
    var createProgress = new ProgressByStepDto(Step.CREATE, 5, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 1, 4, 0, errors);
    var report = new DatasetInfoDto(
        "A review URL will be generated when the dataset has finished processing",
        "A review URL will be generated when the dataset has finished processing",
        5, 4L,
        List.of(createProgress, externalProgress));

    var recordViewCreate = new StepStatistic(Step.CREATE, Status.SUCCESS, 5);
    var recordViewExternal1 = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.SUCCESS, 1);
    var recordViewExternal2 = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.FAIL, 4);
    var errorView1 = new ErrorLogViewImpl(1L, "1", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
    var errorView2 = new ErrorLogViewImpl(1L, "2", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
    var errorView3 = new ErrorLogViewImpl(1L, "3", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
    var errorView4 = new ErrorLogViewImpl(1L, "4", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");

    when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
    when(recordLogRepository.getStepStatistics("1")).thenReturn(
        List.of(recordViewCreate, recordViewExternal1, recordViewExternal2));
    when(errorLogRepository.getByDatasetId("1"))
        .thenReturn(List.of(errorView1, errorView2, errorView3, errorView4));

    var result = service.getReport("1");

    assertReportEquals(report, result);
  }

  @Test
  void getReportWithoutErrors_expectSuccess() {
    var dataset = new DatasetEntity("dataset", 5, Language.NL, Country.NETHERLANDS);
    var createProgress = new ProgressByStepDto(Step.CREATE, 5, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 5, 0, 0, List.of());
    var report = new DatasetInfoDto(
        "A review URL will be generated when the dataset has finished processing",
        "A review URL will be generated when the dataset has finished processing",
        5, 0L,
        List.of(createProgress, externalProgress));

    var recordViewCreate = new StepStatistic(Step.CREATE, Status.SUCCESS, 5);
    var recordViewExternal = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.SUCCESS, 5);

    when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
    when(recordLogRepository.getStepStatistics("1")).thenReturn(
        List.of(recordViewCreate, recordViewExternal));
    when(errorLogRepository.getByDatasetId("1"))
        .thenReturn(List.of());

    var result = service.getReport("1");

    assertReportEquals(report, result);
  }

  @Test
  void getReportCompleted_expectSuccess() {
    var dataset = new DatasetEntity("dataset", 5, Language.NL, Country.NETHERLANDS);
    var createProgress = new ProgressByStepDto(Step.CREATE, 5, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 5, 0, 0, List.of());

    var report = new DatasetInfoDto(
        "https://metis-sandbox/portal/preview/search?q=edm_datasetName:null_dataset*",
        "https://metis-sandbox/portal/publish/search?q=edm_datasetName:null_dataset*", 5, 5L,
        List.of(createProgress, externalProgress));

    var recordViewCreate = new StepStatistic(Step.CREATE, Status.SUCCESS, 5);
    var recordViewExternal = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.SUCCESS, 5);
    var recordViewClose = new StepStatistic(Step.CLOSE, Status.SUCCESS, 5);

    when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
    when(recordLogRepository.getStepStatistics("1")).thenReturn(
        List.of(recordViewCreate, recordViewExternal, recordViewClose));
    when(errorLogRepository.getByDatasetId("1"))
        .thenReturn(List.of());

    var result = service.getReport("1");

    assertReportEquals(report, result);
  }

  @Test
  void getReportCompletedAllErrors_expectSuccess() {
    var dataset = new DatasetEntity("dataset", 5, Language.NL, Country.NETHERLANDS);
    var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
    var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
    var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("1", "2"));
    var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("3", "4", "5"));
    var errors = List.of(error1, error2);
    var createProgress = new ProgressByStepDto(Step.CREATE, 5, 0, 0, List.of());
    var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 0, 5, 0, errors);

    var report = new DatasetInfoDto(
        "All dataset records failed to be processed",
        "All dataset records failed to be processed", 5, 5L,
        List.of(createProgress, externalProgress));

    var recordViewCreate = new StepStatistic(Step.CREATE, Status.SUCCESS, 5);
    var recordViewExternal = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.FAIL, 5);
    var errorView1 = new ErrorLogViewImpl(1L, "1", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
    var errorView2 = new ErrorLogViewImpl(1L, "2", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
    var errorView3 = new ErrorLogViewImpl(1L, "3", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
    var errorView4 = new ErrorLogViewImpl(1L, "4", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
    var errorView5 = new ErrorLogViewImpl(1L, "5", 1, Step.VALIDATE_EXTERNAL, Status.FAIL,
        "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");

    when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
    when(recordLogRepository.getStepStatistics("1")).thenReturn(
        List.of(recordViewCreate, recordViewExternal));
    when(errorLogRepository.getByDatasetId("1"))
        .thenReturn(List.of(errorView1, errorView2, errorView3, errorView4, errorView5));

    var result = service.getReport("1");

    assertReportEquals(report, result);
  }

  @Test
  void getReport_retrieveEmptyDataset_expectSuccess() {
    var datasetEntity = new DatasetEntity("test", 0, Language.valueOf(""), Country.valueOf(""));
    datasetEntity.setDatasetId(1);
    when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
    when(recordLogRepository.getStepStatistics("1")).thenReturn(List.of());

    var expected = new DatasetInfoDto(
        "Dataset is empty",
        "Dataset is empty", 0, 0L, List.of());
    var report = service.getReport("1");
    assertReportEquals(expected, report);
  }

  @Test
  void getReport_failToRetrieveDataset_expectFail() {
    when(datasetRepository.findById(1))
        .thenThrow(new RuntimeException("failed", new Exception()));

    assertThrows(ServiceException.class, () -> service.getReport("1"));
  }

  @Test
  void getReport_failToRetrieveRecords_expectFail() {
    when(datasetRepository.findById(1)).thenReturn(Optional.of(new DatasetEntity("test", 5, Language.NL, Country.NETHERLANDS)));
    when(recordLogRepository.getStepStatistics("1")).thenThrow(new RuntimeException("exception"));

    assertThrows(ServiceException.class, () -> service.getReport("1"));
  }

  @Test
  void getReport_nullDatasetId_expectFail() {
    assertThrows(NullPointerException.class, () -> service.getReport(null));
  }

  private void assertReportEquals(DatasetInfoDto expected, DatasetInfoDto actual) {
    assertEquals(expected.getPortalPreviewUrl(), actual.getPortalPreviewUrl());
    assertEquals(expected.getPortalPublishUrl(), actual.getPortalPublishUrl());
    assertEquals(expected.getProcessedRecords(), actual.getProcessedRecords());
    assertEquals(expected.getTotalRecords(), actual.getTotalRecords());
    assertEquals(expected.getStatus(), actual.getStatus());

    var progressByStepExpected = expected.getProgressByStep();
    var progressByStepActual = actual.getProgressByStep();
    assertEquals(progressByStepExpected.size(), progressByStepActual.size());

    for (int i = 0; i < progressByStepExpected.size(); i++) {
      assertEquals(progressByStepExpected.get(i).getStep(), progressByStepActual.get(i).getStep());
      assertEquals(progressByStepExpected.get(i).getSuccess(),
          progressByStepActual.get(i).getSuccess());
      assertEquals(progressByStepExpected.get(i).getFail(), progressByStepActual.get(i).getFail());
      assertEquals(progressByStepExpected.get(i).getWarn(), progressByStepActual.get(i).getWarn());
      assertEquals(progressByStepExpected.get(i).getTotal(),
          progressByStepActual.get(i).getTotal());

      var errorsByStepExpected = progressByStepExpected.get(i).getErrors();
      var errorsByStepActual = progressByStepActual.get(i).getErrors();
      assertEquals(errorsByStepExpected.size(), errorsByStepActual.size());

      for (int j = 0; j < errorsByStepExpected.size(); j++) {
        assertEquals(errorsByStepExpected.get(i).getErrorMessage(),
            errorsByStepActual.get(i).getErrorMessage());
        assertEquals(errorsByStepExpected.get(i).getType(), errorsByStepActual.get(i).getType());

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