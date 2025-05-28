package eu.europeana.metis.sandbox.service.validationworkflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ValidationWorkflowServiceTest {
    @Mock
    HarvestValidationStep harvestValidationStep;
    @Mock
    ExternalValidationStep externalValidationStep;
    @Mock
    TransformationValidationStep transformationValidationStep;
    @Mock
    InternalValidationValidationStep internalValidationValidationStep;
    @Mock
    DatasetService datasetService;
    @Mock
    RecordRepository recordRepository;
    @Mock
    PatternAnalysisService<FullBatchJobType, ExecutionPoint> patternAnalysisService;
    @Mock
    ExecutionPointService executionPointService;
    @Mock
    LockRegistry lockRegistry;
    @InjectMocks
    ValidationWorkflowService validationWorkflowService;

    @NotNull
    private static ProblemPatternAnalysis getProblemPatternAnalysis() {
        RecordAnalysis recordAnalysis1 = new RecordAnalysis("recordId1", new ArrayList<>());

        return new ProblemPatternAnalysis("rdfAbout",
                List.of(new ProblemPattern(ProblemPatternDescription.P2, 1, List.of(recordAnalysis1)),
                        new ProblemPattern(ProblemPatternDescription.P3, 1, List.of(recordAnalysis1)),
                        new ProblemPattern(ProblemPatternDescription.P5, 1, List.of(recordAnalysis1)),
                        new ProblemPattern(ProblemPatternDescription.P6, 1, List.of(recordAnalysis1)),
                        new ProblemPattern(ProblemPatternDescription.P7, 1, List.of(recordAnalysis1))
                ),
                Set.of("title één", "title twee"));
    }

    @AfterEach
    void setup() {
        reset(patternAnalysisService);
        reset(executionPointService);
        reset(recordRepository);
        reset(lockRegistry);
        reset(harvestValidationStep);
        reset(externalValidationStep);
        reset(transformationValidationStep);
        reset(datasetService);
        reset(internalValidationValidationStep);
    }

    @Test
    void validate_expectSuccess() throws IOException, SerializationException, PatternAnalysisException {
        // given
        MockMultipartFile mockMultipartFile = new MockMultipartFile("valid_record",
                "valid_record.xml", "application/rdf+xml", "mockRDF".getBytes(StandardCharsets.UTF_8));
        Record testRecord = Record.builder()
                .recordId(1L)
                .providerId("providerIs")
                .europeanaId("europeanaId")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content(mockMultipartFile.getInputStream().readAllBytes())
                .build();

        doReturn(new ValidationStepContent(new ValidationResult(Step.VALIDATE_INTERNAL,
                new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                ValidationResult.Status.PASSED), testRecord)).when(internalValidationValidationStep).performStep(any());
        prepareBaseMocks();

        // when
        ValidationWorkflowReport workflowReport = validationWorkflowService.validate(mockMultipartFile, Country.NETHERLANDS, Language.NL);

        // then
        Optional<ValidationResult> result = workflowReport.getValidationResults().stream().filter(f -> f.getStep().equals(Step.VALIDATE_INTERNAL)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.PASSED, result.get().getStatus());
        assertEquals(5, workflowReport.getProblemPatternList().size());
        verifyMocks();
    }

    @Test
    void validate_expectFailure() throws IOException, SerializationException, PatternAnalysisException {
        // given
        MockMultipartFile mockMultipartFile = new MockMultipartFile("invalid_record",
                "invalid_record.xml", "application/rdf+xml", "mockRDF".getBytes(StandardCharsets.UTF_8));
        Record testRecord = Record.builder()
                .recordId(1L)
                .providerId("providerIs")
                .europeanaId("europeanaId")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content(mockMultipartFile.getInputStream().readAllBytes())
                .build();
        doReturn(new ValidationStepContent(new ValidationResult(Step.VALIDATE_INTERNAL,
                new RecordValidationMessage(RecordValidationMessage.Type.ERROR, "Error"),
                ValidationResult.Status.FAILED), testRecord)).when(internalValidationValidationStep).performStep(any());
        prepareBaseMocks();

        // when
        ValidationWorkflowReport workflowReport = validationWorkflowService.validate(mockMultipartFile, Country.NETHERLANDS, Language.NL);

        // then
        Optional<ValidationResult> result = workflowReport.getValidationResults().stream().filter(f -> f.getStep().equals(Step.VALIDATE_INTERNAL)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.FAILED, result.get().getStatus());
        verifyMocks();
    }

    private void prepareBaseMocks() throws PatternAnalysisException {
        when(datasetService.createEmptyDataset(any(), anyString(), any(), any(), any(), any())).thenReturn("datasetId");
        doNothing().when(datasetService).updateNumberOfTotalRecord(anyString(), anyLong());
        RecordEntity recordEntity = new RecordEntity("providerId", "datasetId");
        doReturn(recordEntity).when(recordRepository).save(any());
        doReturn(internalValidationValidationStep.performStep(any())).when(transformationValidationStep).performStep(any());
        doReturn(transformationValidationStep.performStep(any())).when(externalValidationStep).performStep(any());
        doReturn(externalValidationStep.performStep(any())).when(harvestValidationStep).performStep(any());
        ExecutionPoint executionPoint = new ExecutionPoint();
        executionPoint.setExecutionTimestamp(LocalDateTime.now());
        executionPoint.setExecutionPointId(1);
        executionPoint.setDatasetId("datasetId");
        doReturn(Optional.of(executionPoint)).when(executionPointService).getExecutionPoint(anyString(), any());
        Lock lock = new ReentrantLock();
        when(lockRegistry.obtain(any())).thenReturn(lock);
        doNothing().when(patternAnalysisService).finalizeDatasetPatternAnalysis(any());
        Optional<DatasetProblemPatternAnalysis<Step>> optionalStepDatasetProblemPatternAnalysis =
                Optional.of(new DatasetProblemPatternAnalysis<>("datasetId",
                        Step.VALIDATE_INTERNAL,
                        LocalDateTime.now(),
                        getProblemPatternAnalysis().getProblemPatterns()));
        doReturn(optionalStepDatasetProblemPatternAnalysis).when(patternAnalysisService)
                .getDatasetPatternAnalysis(anyString(), any(), any());
    }

    private void verifyMocks() {
        verify(datasetService, times(1)).createEmptyDataset(any(), anyString(), any(), any(), any(), any());
    }
}
