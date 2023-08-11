package eu.europeana.metis.sandbox.service.validationworkflow;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.patternanalysis.ProblemPatternAnalyzer;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
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
    ProblemPatternAnalyzer problemPatternAnalyzer;

    @InjectMocks
    ValidationWorkflowService validationWorkflowService;

    @AfterEach
    void setup() {
        reset(problemPatternAnalyzer);
        reset(harvestValidationStep);
        reset(externalValidationStep);
        reset(transformationValidationStep);
        reset(internalValidationValidationStep);
    }

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

    @Test
    void validate_validRecordFile_expectSuccess() throws IOException, SerializationException {
        // given
        MockMultipartFile mockMultipartFile = new MockMultipartFile("valid_record",
                "valid_record.xml", "application/rdf+xml", "mockRDF".getBytes(StandardCharsets.UTF_8));
        ProblemPatternAnalysis problemPatternAnalysis = getProblemPatternAnalysis();
        doReturn(new ValidationResult("success", ValidationResult.Status.PASSED)).when(internalValidationValidationStep).validate(any());
        doReturn(internalValidationValidationStep.validate(any())).when(transformationValidationStep).validate(any());
        doReturn(transformationValidationStep.validate(any())).when(externalValidationStep).validate(any());
        doReturn(externalValidationStep.validate(any())).when(harvestValidationStep).validate(any());
        when(problemPatternAnalyzer.analyzeRecord(anyString())).thenReturn(problemPatternAnalysis);

        // when
        ValidationWorkflowReport workflowReport = validationWorkflowService.validate(mockMultipartFile, Country.NETHERLANDS, Language.NL);

        // then
        assertEquals(ValidationResult.Status.PASSED, workflowReport.getResult().getStatus());
        assertEquals("success", workflowReport.getResult().getMessage());
        assertEquals(5, workflowReport.getProblemPatternList().size());
    }

    @Test
    void validate_invalidRecordFile_expectFailure() throws IOException, SerializationException {
        // given
        MockMultipartFile mockMultipartFile = new MockMultipartFile("invalid_record",
                "invalid_record.xml", "application/rdf+xml", "mockRDF".getBytes(StandardCharsets.UTF_8));
        doReturn(new ValidationResult("internal validation step fail", ValidationResult.Status.FAILED)).when(internalValidationValidationStep).validate(any());
        doReturn(internalValidationValidationStep.validate(any())).when(transformationValidationStep).validate(any());
        doReturn(transformationValidationStep.validate(any())).when(externalValidationStep).validate(any());
        doReturn(externalValidationStep.validate(any())).when(harvestValidationStep).validate(any());
        doReturn(new ProblemPatternAnalysis("rdf", List.of(), Set.of())).when(problemPatternAnalyzer).analyzeRecord(anyString());

        // when
        ValidationWorkflowReport workflowReport = validationWorkflowService.validate(mockMultipartFile, Country.NETHERLANDS, Language.NL);

        // then
        assertEquals(ValidationResult.Status.FAILED, workflowReport.getResult().getStatus());
        assertEquals("internal validation step fail", workflowReport.getResult().getMessage());
    }

}
