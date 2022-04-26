package eu.europeana.metis.sandbox.controller;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(PatternAnalysisController.class)
class PatternAnalysisControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PatternAnalysisService<Step> mockPatternAnalysisService;

    @MockBean
    private ExecutionPointService mockExecutionPointService;

    @MockBean
    private RecordLogService mockRecordLogService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    @Test
    void getDatasetAnalysis_expectSuccess() throws Exception {

        LocalDateTime executionTimestamp = LocalDateTime.now();
        List<ProblemPattern> problemPatternList = new ArrayList<>();
        DatasetProblemPatternAnalysis<Step> datasetProblemPatternAnalysis =
                new DatasetProblemPatternAnalysis<>("datasetId", Step.VALIDATE_INTERNAL, executionTimestamp, problemPatternList);

        when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", Step.VALIDATE_INTERNAL, executionTimestamp))
                .thenReturn(Optional.of(datasetProblemPatternAnalysis));

        mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId")
                        .param("executionTimestamp", String.valueOf(executionTimestamp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetId", is("datasetId")))
                .andExpect(jsonPath("$.executionStep", is(Step.VALIDATE_INTERNAL.value())))
                .andExpect(jsonPath("$.executionTimestamp", is(executionTimestamp.toString())))
                .andExpect(jsonPath("$.problemPatternList", is(Collections.EMPTY_LIST)));
    }

    @Test
    void getDatasetAnalysis_getEmptyResult_expectSuccess() throws Exception {

        LocalDateTime executionTimestamp = LocalDateTime.now();
        when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", Step.VALIDATE_INTERNAL, executionTimestamp))
                .thenReturn(Optional.empty());

        mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId")
                        .param("executionTimestamp", String.valueOf(executionTimestamp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetId", is("0")));
    }

    @Test
    void getRecordPatternAnalysis_expectSuccess() throws Exception {


        ProblemPattern problemPattern = new ProblemPattern(ProblemPatternDescription.P2, 0, new ArrayList<>());
        RecordLogEntity mockRecordLogEntity = mock(RecordLogEntity.class);
        List<ProblemPattern> problemPatternList = List.of(problemPattern);
        String recordContent = IOUtils.toString(
                new FileInputStream("src/test/resources/record.problempatterns/record_pattern_problem.xml"),
                StandardCharsets.UTF_8);

        when(mockRecordLogService.getRecordLogEntity("recordId", "datasetId", Step.VALIDATE_INTERNAL)).thenReturn(mockRecordLogEntity);
        when(mockRecordLogEntity.getContent()).thenReturn(recordContent);
        when(mockPatternAnalysisService.getRecordPatternAnalysis(any(RDF.class)))
                .thenReturn(problemPatternList);

        mvc.perform(get("/pattern-analysis/{id}/get-record-pattern-analysis", "datasetId")
                        .param("recordId", "recordId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordOccurrences", is(0)))
                .andExpect(jsonPath("$[0].recordAnalysisList", is(Collections.EMPTY_LIST)))
                .andExpect(jsonPath("$[0].problemPatternDescription.problemPatternId", is("P2")))
                .andExpect(jsonPath("$[0].problemPatternDescription.problemPatternSeverity", is("WARNING")))
                .andExpect(jsonPath("$[0].problemPatternDescription.problemPatternQualityDimension", is("CONCISENESS")));

    }

    @Test
    void getAllExecutionTimestamps_expectSuccess() throws Exception {

        when(mockExecutionPointService.getAllExecutionTimestamps()).thenReturn(new HashSet<>(List.of(LocalDateTime.from(FORMATTER.parse("2022-04-19T15:53:57.377423")),
                LocalDateTime.from(FORMATTER.parse("2022-04-21T12:19:35.339562")),
                LocalDateTime.from(FORMATTER.parse("2022-04-19T15:44:10.634167")))));

        mvc.perform(get("/pattern-analysis/execution-timestamps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", is("2022-04-19T15:53:57.377423")))
                .andExpect(jsonPath("$[1]", is("2022-04-21T12:19:35.339562")))
                .andExpect(jsonPath("$[2]", is("2022-04-19T15:44:10.634167")));
    }
}