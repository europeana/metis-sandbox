package eu.europeana.metis.sandbox.controller;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.service.validationworkflow.RecordValidationMessage;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationResult;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationWorkflowReport;
import eu.europeana.metis.sandbox.service.validationworkflow.ValidationWorkflowService;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static eu.europeana.metis.sandbox.common.locale.Country.NETHERLANDS;
import static eu.europeana.metis.sandbox.common.locale.Language.NL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ValidationController.class)
class ValidationControllerTest {
    @MockBean
    ValidationWorkflowService validationWorkflowService;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

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
    void validate_expectSuccess() throws Exception {
        Path testRecordPath = Paths.get("src", "test", "resources", "record", "validation", "valid_record.xml");
        MockMultipartFile mockRecordFile = new MockMultipartFile("recordToValidate",
                "valid_record.xml", "application/rdf+xml", Files.newInputStream(testRecordPath));
        ValidationWorkflowReport workflowReport = new ValidationWorkflowReport(List.of(
                new ValidationResult(Step.HARVEST_FILE,
                        new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                        ValidationResult.Status.PASSED),
                new ValidationResult(Step.VALIDATE_EXTERNAL,
                        new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                        ValidationResult.Status.PASSED),
                new ValidationResult(Step.TRANSFORM,
                        new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                        ValidationResult.Status.PASSED),
                new ValidationResult(Step.VALIDATE_INTERNAL,
                        new RecordValidationMessage(RecordValidationMessage.Type.INFO, "success"),
                        ValidationResult.Status.PASSED)),
                getProblemPatternAnalysis().getProblemPatterns());
        when(validationWorkflowService.validate(any(MultipartFile.class), any(), any())).thenReturn(workflowReport);
        mvc.perform(multipart("/record/validation", "test")
                        .file(mockRecordFile)
                        .param("country", NETHERLANDS.name())
                        .param("language", NL.name()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }
}
