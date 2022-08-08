package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.*;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogView;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;

import java.time.LocalDateTime;
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

    @Mock
    private RecordRepository recordRepository;

    @InjectMocks
    private DatasetReportServiceImpl service;

    @BeforeEach
    void setup() {
        setField(service, "portalPublishDatasetUrl",
                "https://metis-sandbox/portal/publish/search?q=edm_datasetName:");
    }

    @Test
    void getReportWithErrors_expectSuccess() {
        var dataset = new DatasetEntity("dataset", 5L, Language.NL, Country.NETHERLANDS, false);
        var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
        var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
        var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("europeanaId1 | providerId1", "europeanaId2 | providerId2"));
        var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("europeanaId3 | providerId3", "europeanaId4 | providerId4"));
        var errors = List.of(error1, error2);
        var createProgress = new ProgressByStepDto(Step.HARVEST_ZIP, 5, 0, 0, List.of());
        var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 1, 4, 0, errors);
        var report = new ProgressInfoDto(
                "A review URL will be generated when the dataset has finished processing.",
                5L, 4L,
                List.of(createProgress, externalProgress),
                new DatasetInfoDto("", "", LocalDateTime.now(), Language.NL, Country.NETHERLANDS,
                        false, false), "", null);

        var recordViewCreate = new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS, 5L);
        var recordViewExternal1 = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.SUCCESS, 1L);
        var recordViewExternal2 = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.FAIL, 4L);
        var errorView1 = new ErrorLogViewImpl(1L, getTestRecordEntity(1L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
        var errorView2 = new ErrorLogViewImpl(1L, getTestRecordEntity(2L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
        var errorView3 = new ErrorLogViewImpl(1L, getTestRecordEntity(3L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
        var errorView4 = new ErrorLogViewImpl(1L, getTestRecordEntity(4L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");

        when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
        when(recordLogRepository.getStepStatistics("1")).thenReturn(
                List.of(recordViewCreate, recordViewExternal1, recordViewExternal2));
        when(errorLogRepository.getByRecordIdDatasetId("1"))
                .thenReturn(List.of(errorView1, errorView2, errorView3, errorView4));

        var result = service.getReport("1");

        assertReportEquals(report, result);
    }

    @Test
    void getReportWithoutErrors_expectSuccess() {
        var dataset = new DatasetEntity("dataset", 5L, Language.NL, Country.NETHERLANDS, false);
        var createProgress = new ProgressByStepDto(Step.HARVEST_ZIP, 5, 0, 0, List.of());
        var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 5, 0, 0, List.of());
        var report = new ProgressInfoDto(
                "A review URL will be generated when the dataset has finished processing.",
                5L, 0L,
                List.of(createProgress, externalProgress),
                new DatasetInfoDto("", "", LocalDateTime.now(), null, null, false, false), "", null);

        var recordViewCreate = new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS, 5L);
        var recordViewExternal = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.SUCCESS, 5L);

        when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
        when(recordLogRepository.getStepStatistics("1")).thenReturn(
                List.of(recordViewCreate, recordViewExternal));
        when(errorLogRepository.getByRecordIdDatasetId("1"))
                .thenReturn(List.of());

        var result = service.getReport("1");

        assertReportEquals(report, result);
    }

    @Test
    void getReportCompleted_expectSuccess() {
        var dataset = new DatasetEntity("dataset", 5L, Language.NL, Country.NETHERLANDS, false);
        var createProgress = new ProgressByStepDto(Step.HARVEST_ZIP, 5, 0, 0, List.of());
        var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 5, 0, 0, List.of());
        var publishProgress = new ProgressByStepDto(Step.PUBLISH, 5, 0, 0, List.of());
        var tiersZeroInfo = new TiersZeroInfo(new TierStatistics(2, List.of("providerId1", "providerId2")),
                new TierStatistics(3, List.of("providerId1", "providerId2", "provider3")));
        var report = new ProgressInfoDto(
                "https://metis-sandbox/portal/publish/search?q=edm_datasetName:null_dataset*", 5L, 5L,
                List.of(createProgress, externalProgress, publishProgress),
                new DatasetInfoDto("", "", LocalDateTime.now(), Language.NL, Country.NETHERLANDS,
                        false, false), "", tiersZeroInfo);

        var recordViewCreate = new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS, 5L);
        var recordViewExternal = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.SUCCESS, 5L);
        var recordViewPublish = new StepStatistic(Step.PUBLISH, Status.SUCCESS, 5L);
        var recordViewClose = new StepStatistic(Step.CLOSE, Status.SUCCESS, 5L);

        when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
        when(recordLogRepository.getStepStatistics("1")).thenReturn(
                List.of(recordViewCreate, recordViewExternal, recordViewPublish, recordViewClose));
        when(errorLogRepository.getByRecordIdDatasetId("1"))
                .thenReturn(List.of());
        when(recordRepository.findByDatasetIdAndContentTier("1", MediaTier.T0.toString()))
                .thenReturn(List.of("providerId1", "providerId2"));
        when(recordRepository.findByDatasetIdAndMetadataTier("1", MetadataTier.T0.toString()))
                .thenReturn(List.of("providerId1", "providerId2", "provider3"));

        var result = service.getReport("1");

        assertReportEquals(report, result);
    }

    @Test
    void getReportCompletedAllErrors_expectSuccess() {
        var dataset = new DatasetEntity("dataset", 5L, Language.NL, Country.NETHERLANDS, false);
        var message1 = "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.";
        var message2 = "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.";
        var error1 = new ErrorInfoDto(message1, Status.FAIL, List.of("europeanaId1 | providerId1", "europeanaId2 | providerId2"));
        var error2 = new ErrorInfoDto(message2, Status.FAIL, List.of("europeanaId3 | providerId3", "europeanaId4 | providerId4", "europeanaId5 | providerId5"));
        var errors = List.of(error1, error2);
        var createProgress = new ProgressByStepDto(Step.HARVEST_ZIP, 5, 0, 0, List.of());
        var externalProgress = new ProgressByStepDto(Step.VALIDATE_EXTERNAL, 0, 5, 0, errors);

        var report = new ProgressInfoDto(
                "", 5L, 5L,
                List.of(createProgress, externalProgress),
                new DatasetInfoDto("", "", LocalDateTime.now(), Language.NL, Country.NETHERLANDS,
                        false, false), "All dataset records failed to be processed.", null);

        var recordViewCreate = new StepStatistic(Step.HARVEST_ZIP, Status.SUCCESS, 5L);
        var recordViewExternal = new StepStatistic(Step.VALIDATE_EXTERNAL, Status.FAIL, 5L);

        var errorView1 = new ErrorLogViewImpl(1L, getTestRecordEntity(1L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
        var errorView2 = new ErrorLogViewImpl(1L, getTestRecordEntity(2L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.4: Attribute 'resource' must appear on element 'edm:object'.");
        var errorView3 = new ErrorLogViewImpl(1L, getTestRecordEntity(3L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
        var errorView4 = new ErrorLogViewImpl(1L, getTestRecordEntity(4L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");
        var errorView5 = new ErrorLogViewImpl(1L, getTestRecordEntity(5L), Step.VALIDATE_EXTERNAL, Status.FAIL,
                "cvc-complex-type.2.4.b: The content of element 'edm:ProvidedCHO' is not complete.");

        when(datasetRepository.findById(1)).thenReturn(Optional.of(dataset));
        when(recordLogRepository.getStepStatistics("1")).thenReturn(
                List.of(recordViewCreate, recordViewExternal));
        when(errorLogRepository.getByRecordIdDatasetId("1"))
                .thenReturn(List.of(errorView1, errorView2, errorView3, errorView4, errorView5));

        var result = service.getReport("1");

        assertReportEquals(report, result);
    }

    @Test
    void getReport_retrieveEmptyDataset_expectSuccess() {
        var datasetEntity = new DatasetEntity("test", 0L, Language.NL, Country.NETHERLANDS, false);
        datasetEntity.setDatasetId(1);
        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
        when(recordLogRepository.getStepStatistics("1")).thenReturn(List.of());

        var expected = new ProgressInfoDto(
                "", 0L, 0L, List.of(),
                new DatasetInfoDto("", "", LocalDateTime.now(), null, null, false, false),
                "Dataset is empty.", null);
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
        when(datasetRepository.findById(1)).thenReturn(Optional.of(new DatasetEntity("test", 5L, Language.NL, Country.NETHERLANDS, false)));
        when(recordLogRepository.getStepStatistics("1")).thenThrow(new RuntimeException("exception"));

        assertThrows(ServiceException.class, () -> service.getReport("1"));
    }

    @Test
    void getReport_nullDatasetId_expectFail() {
        assertThrows(NullPointerException.class, () -> service.getReport(null));
    }

    @Test
    void getReport_HarvestingDataset_expectSuccess() {
        var datasetEntity = new DatasetEntity("test", null, Language.NL, Country.NETHERLANDS, false);
        datasetEntity.setDatasetId(1);
        when(datasetRepository.findById(1)).thenReturn(Optional.of(datasetEntity));
        when(recordLogRepository.getStepStatistics("1")).thenReturn(List.of());

        var expected = new ProgressInfoDto(
            "Harvesting dataset identifiers and records.", null, 0L, List.of(),
            new DatasetInfoDto("", "", LocalDateTime.now(), null, null, false, false), "", null);
        var report = service.getReport("1");

        assertReportEquals(expected, report);
    }

    private void assertReportEquals(ProgressInfoDto expected, ProgressInfoDto actual) {
        assertEquals(expected.getPortalPublishUrl(), actual.getPortalPublishUrl());
        assertEquals(expected.getProcessedRecords(), actual.getProcessedRecords());
        assertEquals(expected.getTotalRecords(), actual.getTotalRecords());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getErrorType(), actual.getErrorType());
        assertEquals(expected.getTiersZeroInfo(), actual.getTiersZeroInfo());

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

    private static RecordEntity getTestRecordEntity(final Long recordId) {
        RecordEntity recordEntity = new RecordEntity("europeanaId" + recordId.toString(),
                "providerId" + recordId, recordId.toString(), "", "");
        recordEntity.setId(recordId);
        return recordEntity;
    }

    private static class ErrorLogViewImpl implements ErrorLogView {

        private final Long id;
        private final RecordEntity recordId;
        private final Step step;
        private final Status status;
        private final String message;

        public ErrorLogViewImpl(Long id, RecordEntity recordId,
                                Step step, Status status, String message) {
            this.id = id;
            this.recordId = recordId;
            this.step = step;
            this.status = status;
            this.message = message;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public RecordEntity getRecordId() {
            return recordId;
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
