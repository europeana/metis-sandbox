package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

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
    LockRegistry lockRegistry;
    @Mock
    ExecutionPointService executionPointService;
    @Mock
    PatternAnalysisService<Step, ExecutionPoint> patternAnalysisService;
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
        reset(internalValidationValidationStep);
    }

    @Test
    void validate_validRecordFile_expectSuccess() throws IOException, SerializationException {
        // given
        MockMultipartFile mockMultipartFile = new MockMultipartFile("valid_record",
                "valid_record.xml", "application/rdf+xml", "mockRDF".getBytes(StandardCharsets.UTF_8));
        ProblemPatternAnalysis problemPatternAnalysis = getProblemPatternAnalysis();
        doReturn(List.of(new ValidationResult(Step.VALIDATE_INTERNAL,
                new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                ValidationResult.Status.PASSED))).when(internalValidationValidationStep).validate(any());
        doReturn(internalValidationValidationStep.validate(any())).when(transformationValidationStep).validate(any());
        doReturn(transformationValidationStep.validate(any())).when(externalValidationStep).validate(any());
        doReturn(externalValidationStep.validate(any())).when(harvestValidationStep).validate(any());


        // when
        ValidationWorkflowReport workflowReport = validationWorkflowService.validate(mockMultipartFile, Country.NETHERLANDS, Language.NL);

        // then
        Optional<ValidationResult> result = workflowReport.getValidationResults().stream().filter(f -> f.getStep().equals(Step.VALIDATE_INTERNAL)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.PASSED, result.get().getStatus());
        assertEquals(5, workflowReport.getProblemPatternList().size());
    }

    @Test
    void validate_invalidRecordFile_expectFailure() throws IOException, SerializationException {
        // given
        MockMultipartFile mockMultipartFile = new MockMultipartFile("invalid_record",
                "invalid_record.xml", "application/rdf+xml", "mockRDF".getBytes(StandardCharsets.UTF_8));
        doReturn(List.of(new ValidationResult(Step.VALIDATE_INTERNAL,
                new RecordValidationMessage(RecordValidationMessage.Type.ERROR, "Error"),
                ValidationResult.Status.FAILED))).when(internalValidationValidationStep).validate(any());
        doReturn(internalValidationValidationStep.validate(any())).when(transformationValidationStep).validate(any());
        doReturn(transformationValidationStep.validate(any())).when(externalValidationStep).validate(any());
        doReturn(externalValidationStep.validate(any())).when(harvestValidationStep).validate(any());
        // doReturn(new ProblemPatternAnalysis("rdf", List.of(), Set.of())).when(problemPatternAnalyzer).analyzeRecord(anyString());

        // when
        ValidationWorkflowReport workflowReport = validationWorkflowService.validate(mockMultipartFile, Country.NETHERLANDS, Language.NL);

        // then
        Optional<ValidationResult> result = workflowReport.getValidationResults().stream().filter(f -> f.getStep().equals(Step.VALIDATE_INTERNAL)).findFirst();
        assertTrue(result.isPresent());
        assertEquals(ValidationResult.Status.FAILED, result.get().getStatus());
    }

}
