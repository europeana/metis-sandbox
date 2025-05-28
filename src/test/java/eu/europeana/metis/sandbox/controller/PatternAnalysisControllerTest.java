package eu.europeana.metis.sandbox.controller;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.ControllerErrorHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.sandbox.service.problempatterns.ExecutionPointService;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import eu.europeana.metis.schema.jibx.RDF;
import eu.europeana.patternanalysis.PatternAnalysisService;
import eu.europeana.patternanalysis.exception.PatternAnalysisException;
import eu.europeana.patternanalysis.view.DatasetProblemPatternAnalysis;
import eu.europeana.patternanalysis.view.ProblemOccurrence;
import eu.europeana.patternanalysis.view.ProblemPattern;
import eu.europeana.patternanalysis.view.ProblemPatternDescription;
import eu.europeana.patternanalysis.view.RecordAnalysis;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(PatternAnalysisController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, PatternAnalysisController.class, SecurityConfig.class, ControllerErrorHandler.class})
class PatternAnalysisControllerTest {

  @MockBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockBean
  private PatternAnalysisService<FullBatchJobType, ExecutionPoint> mockPatternAnalysisService;

  @MockBean
  private ExecutionPointService mockExecutionPointService;

  @MockBean
  private RecordLogService mockRecordLogService;

  @MockBean
  private DatasetReportService mockDatasetReportService;

  @MockBean
  private LockRegistry lockRegistry;

  @MockBean
  JwtDecoder jwtDecoder;

  private static MockMvc mvc;
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

  @BeforeAll
  static void setup(WebApplicationContext context) {
    mvc = MockMvcBuilders.webAppContextSetup(context)
                             .apply(SecurityMockMvcConfigurers.springSecurity())
                             .defaultRequest(get("/"))
                             .build();
  }

  @BeforeEach
  void resetMocks() {
    reset(mockPatternAnalysisService, mockExecutionPointService, mockRecordLogService,
        mockDatasetReportService, lockRegistry, jwtDecoder);
  }

  @Test
  void getDatasetAnalysis_expectSuccess() throws Exception {

    // Set up the tests
    LocalDateTime executionTimestamp = LocalDateTime.now();
    ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setExecutionTimestamp(executionTimestamp);
    executionPoint.setDatasetId("datasetId");
    executionPoint.setExecutionPointId(1);
    List<ProblemPattern> problemPatternList = new ArrayList<>();
    DatasetProblemPatternAnalysis<FullBatchJobType> datasetProblemPatternAnalysis =
        new DatasetProblemPatternAnalysis<>("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp, problemPatternList);
    when(mockExecutionPointService.getExecutionPoint("datasetId", FullBatchJobType.VALIDATE_INTERNAL.toString()))
        .thenReturn(Optional.of(executionPoint));
    when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp))
        .thenReturn(Optional.of(datasetProblemPatternAnalysis));
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());

    // First check with dataset that is still processing
    final ProgressInfoDto inProgressInfo = new ProgressInfoDto("", 1L, 0L,
        Collections.emptyList(), false, "", emptyList(), null);
    when(mockDatasetReportService.getReport("datasetId")).thenReturn(inProgressInfo);
    assertNotEquals(Status.COMPLETED, inProgressInfo.getStatus());

    mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.datasetId", is("datasetId")))
        .andExpect(jsonPath("$.executionStep", is(FullBatchJobType.VALIDATE_INTERNAL.name())))
        .andExpect(jsonPath("$.executionTimestamp", is(executionTimestamp.toString())))
        .andExpect(jsonPath("$.problemPatternList", is(Collections.EMPTY_LIST)))
        .andExpect(jsonPath("$.analysisStatus", is(ProblemPatternAnalysisStatus.IN_PROGRESS.name())));
    verify(mockPatternAnalysisService, never()).finalizeDatasetPatternAnalysis(any());

    // Now check with finalized dataset.
    final ProgressInfoDto completedInfo = new ProgressInfoDto("", 1L, 1L,
        Collections.emptyList(), false, "", emptyList(), null);
    when(mockDatasetReportService.getReport("datasetId")).thenReturn(completedInfo);
    assertEquals(Status.COMPLETED, completedInfo.getStatus());

    mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.datasetId", is("datasetId")))
        .andExpect(jsonPath("$.executionStep", is(Step.VALIDATE_INTERNAL.name())))
        .andExpect(jsonPath("$.executionTimestamp", is(executionTimestamp.toString())))
        .andExpect(jsonPath("$.problemPatternList", is(Collections.EMPTY_LIST)))
        .andExpect(jsonPath("$.analysisStatus", is(ProblemPatternAnalysisStatus.FINALIZED.name())));
    verify(mockPatternAnalysisService, times(1)).finalizeDatasetPatternAnalysis(executionPoint);
  }

  @Test
  void getDatasetAnalysis_withProblemPatternList_checkSorting_expectSuccess() throws Exception {
    LocalDateTime executionTimestamp = LocalDateTime.now();
    ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setExecutionTimestamp(executionTimestamp);
    executionPoint.setDatasetId("datasetId");
    executionPoint.setExecutionPointId(1);

    List<ProblemPattern> problemPatternList = new ArrayList<>();
    RecordAnalysis recordAnalysis1 = new RecordAnalysis("recordId1", new ArrayList<>());
    RecordAnalysis recordAnalysis2 = new RecordAnalysis("recordId2", new ArrayList<>());
    RecordAnalysis recordAnalysis3 = new RecordAnalysis("recordId3", new ArrayList<>());
    problemPatternList.add(new ProblemPattern(ProblemPatternDescription.P2, 2,
        List.of(recordAnalysis2, recordAnalysis1)));
    problemPatternList.add(new ProblemPattern(ProblemPatternDescription.P3, 3,
        List.of(recordAnalysis2, recordAnalysis3, recordAnalysis1)));

    DatasetProblemPatternAnalysis<FullBatchJobType> datasetProblemPatternAnalysis =
        new DatasetProblemPatternAnalysis<>("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp, problemPatternList);
    when(mockExecutionPointService.getExecutionPoint("datasetId", FullBatchJobType.VALIDATE_INTERNAL.toString()))
        .thenReturn(Optional.of(executionPoint));
    when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp))
        .thenReturn(Optional.of(datasetProblemPatternAnalysis));
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());

    when(mockDatasetReportService.getReport("datasetId")).thenReturn(
        new ProgressInfoDto("", 1L, 1L, Collections.emptyList(), false, "", emptyList(), null));

    mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.datasetId", is("datasetId")))
       .andExpect(jsonPath("$.executionStep", is(FullBatchJobType.VALIDATE_INTERNAL.name())))
       .andExpect(jsonPath("$.executionTimestamp", is(executionTimestamp.toString())))
       .andExpect(jsonPath("$.analysisStatus", is(ProblemPatternAnalysisStatus.FINALIZED.name())))
       .andExpect(jsonPath("$.problemPatternList[0].problemPatternDescription.problemPatternId",
           is(ProblemPatternDescription.ProblemPatternId.P2.toString())))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[0].recordId", is("recordId1")))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[1].recordId", is("recordId2")))
       .andExpect(jsonPath("$.problemPatternList[1].problemPatternDescription.problemPatternId",
           is(ProblemPatternDescription.ProblemPatternId.P3.toString())))
       .andExpect(jsonPath("$.problemPatternList[1].recordAnalysisList[0].recordId", is("recordId1")))
       .andExpect(jsonPath("$.problemPatternList[1].recordAnalysisList[1].recordId", is("recordId2")))
       .andExpect(jsonPath("$.problemPatternList[1].recordAnalysisList[2].recordId", is("recordId3")));
    verify(mockPatternAnalysisService, times(1)).finalizeDatasetPatternAnalysis(executionPoint);
  }

  @Test
  void getDatasetAnalysis_finalize_fail_expectSuccess() throws Exception {
    LocalDateTime executionTimestamp = LocalDateTime.now();
    ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setExecutionTimestamp(executionTimestamp);
    executionPoint.setDatasetId("datasetId");
    executionPoint.setExecutionPointId(1);
    List<ProblemPattern> problemPatternList = new ArrayList<>();
    DatasetProblemPatternAnalysis<FullBatchJobType> datasetProblemPatternAnalysis =
        new DatasetProblemPatternAnalysis<>("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp, problemPatternList);
    when(mockExecutionPointService.getExecutionPoint("datasetId", FullBatchJobType.VALIDATE_INTERNAL.toString()))
        .thenReturn(Optional.of(executionPoint));
    when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp))
        .thenReturn(Optional.of(datasetProblemPatternAnalysis));
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());

    when(mockDatasetReportService.getReport("datasetId")).thenReturn(
        new ProgressInfoDto("", 1L, 1L, Collections.emptyList(), false, "", emptyList(), null));
    doThrow(PatternAnalysisException.class).when(mockPatternAnalysisService).finalizeDatasetPatternAnalysis(executionPoint);

    mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.datasetId", is("datasetId")))
        .andExpect(jsonPath("$.executionStep", is(FullBatchJobType.VALIDATE_INTERNAL.name())))
        .andExpect(jsonPath("$.executionTimestamp", is(executionTimestamp.toString())))
        .andExpect(jsonPath("$.problemPatternList", is(Collections.EMPTY_LIST)))
        .andExpect(jsonPath("$.analysisStatus", is(ProblemPatternAnalysisStatus.ERROR.name())));
    verify(mockPatternAnalysisService, times(1)).finalizeDatasetPatternAnalysis(executionPoint);
  }

  @Test
  void getDatasetAnalysis_getEmptyResult_expectSuccess() throws Exception {
    LocalDateTime executionTimestamp = LocalDateTime.now();
    when(mockExecutionPointService.getExecutionPoint("datasetId", FullBatchJobType.VALIDATE_INTERNAL.toString()))
        .thenReturn(Optional.empty());
    when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp))
        .thenReturn(Optional.empty());

    mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.datasetId", is("datasetId")))
        .andExpect(jsonPath("$.analysisStatus", is(ProblemPatternAnalysisStatus.PENDING.name())));
    verify(mockPatternAnalysisService, never()).finalizeDatasetPatternAnalysis(any());
  }

  @Test
  void getDatasetAnalysis_executionPoint_getEmptyResult_expectSuccess() throws Exception {
    LocalDateTime executionTimestamp = LocalDateTime.now();
    ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setExecutionTimestamp(executionTimestamp);
    executionPoint.setDatasetId("datasetId");
    executionPoint.setExecutionPointId(1);
    when(mockExecutionPointService.getExecutionPoint("datasetId", FullBatchJobType.VALIDATE_INTERNAL.toString()))
        .thenReturn(Optional.of(executionPoint));
    when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp))
        .thenReturn(Optional.empty());
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());

    when(mockDatasetReportService.getReport("datasetId")).thenReturn(
        new ProgressInfoDto("", 1L, 1L, Collections.emptyList(), false, "", emptyList(), null));

    mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.datasetId", is("datasetId")))
        .andExpect(jsonPath("$.analysisStatus", is(ProblemPatternAnalysisStatus.ERROR.name())));
  }

  @Test
  void getRecordPatternAnalysis_expectSuccess() throws Exception {
    ProblemPattern problemPattern = new ProblemPattern(ProblemPatternDescription.P2, 0, new ArrayList<>());
    RecordLogEntity mockRecordLogEntity = mock(RecordLogEntity.class);
    List<ProblemPattern> problemPatternList = List.of(problemPattern);
    String recordContent = IOUtils.toString(
        new FileInputStream("src/test/resources/record.problempatterns/record_pattern_problem.xml"),
        StandardCharsets.UTF_8);

    when(mockRecordLogService.getRecordLogEntity("recordId", "datasetId", Step.VALIDATE_INTERNAL)).thenReturn(
        mockRecordLogEntity);
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
  void getRecordPatternAnalysis_notFound_expectSuccess() throws Exception {
    when(mockRecordLogService.getRecordLogEntity("recordId", "datasetId", Step.VALIDATE_INTERNAL)).thenReturn(
        null);
    mvc.perform(get("/pattern-analysis/{id}/get-record-pattern-analysis", "datasetId")
           .param("recordId", "recordId"))
       .andExpect(status().isNotFound());
  }

  @Test
  void getAllExecutionTimestamps_expectSuccess() throws Exception {
    when(mockExecutionPointService.getAllExecutionTimestamps()).thenReturn(
        new HashSet<>(List.of(LocalDateTime.from(FORMATTER.parse("2022-04-19T15:53:57.377423")),
            LocalDateTime.from(FORMATTER.parse("2022-04-21T12:19:35.339562")),
            LocalDateTime.from(FORMATTER.parse("2022-04-19T15:44:10.634167")))));

    mvc.perform(get("/pattern-analysis/execution-timestamps"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$[0]", is("2022-04-19T15:53:57.377423")))
       .andExpect(jsonPath("$[1]", is("2022-04-21T12:19:35.339562")))
       .andExpect(jsonPath("$[2]", is("2022-04-19T15:44:10.634167")));
  }

  @Test
  void getDatasetAnalysis_withCleanMessageReportP7AndSorted_expectSuccess() throws Exception {
    LocalDateTime executionTimestamp = LocalDateTime.now();
    ExecutionPoint executionPoint = new ExecutionPoint();
    executionPoint.setExecutionTimestamp(executionTimestamp);
    executionPoint.setDatasetId("datasetId");
    executionPoint.setExecutionPointId(1);

    List<ProblemPattern> problemPatternList = new ArrayList<>();
    RecordAnalysis recordAnalysis1 = new RecordAnalysis("recordId1", List.of(new ProblemOccurrence("text1", List.of())));
    RecordAnalysis recordAnalysis2 = new RecordAnalysis("recordId2", List.of(new ProblemOccurrence("text2", List.of())));
    RecordAnalysis recordAnalysis3 = new RecordAnalysis("recordId3", List.of(new ProblemOccurrence("text3", List.of())));
    problemPatternList.add(new ProblemPattern(ProblemPatternDescription.P7, 3,
        List.of(recordAnalysis2, recordAnalysis1, recordAnalysis3)));

    DatasetProblemPatternAnalysis<FullBatchJobType> datasetProblemPatternAnalysis =
        new DatasetProblemPatternAnalysis<>("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp, problemPatternList);
    when(mockExecutionPointService.getExecutionPoint("datasetId", FullBatchJobType.VALIDATE_INTERNAL.toString()))
        .thenReturn(Optional.of(executionPoint));
    when(mockPatternAnalysisService.getDatasetPatternAnalysis("datasetId", FullBatchJobType.VALIDATE_INTERNAL, executionTimestamp))
        .thenReturn(Optional.of(datasetProblemPatternAnalysis));
    when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());

    when(mockDatasetReportService.getReport("datasetId")).thenReturn(
        new ProgressInfoDto("", 1L, 1L, Collections.emptyList(), false, "", emptyList(), null));

    mvc.perform(get("/pattern-analysis/{id}/get-dataset-pattern-analysis", "datasetId"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.datasetId", is("datasetId")))
       .andExpect(jsonPath("$.executionStep", is(FullBatchJobType.VALIDATE_INTERNAL.name())))
       .andExpect(jsonPath("$.executionTimestamp", is(executionTimestamp.toString())))
       .andExpect(jsonPath("$.analysisStatus", is(ProblemPatternAnalysisStatus.FINALIZED.name())))
       .andExpect(jsonPath("$.problemPatternList[0].problemPatternDescription.problemPatternId",
           is(ProblemPatternDescription.ProblemPatternId.P7.toString())))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[0].recordId", is("recordId1")))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[0].problemOccurrenceList[0].messageReport", is("")))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[1].recordId", is("recordId2")))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[1].problemOccurrenceList[0].messageReport", is("")))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[2].recordId", is("recordId3")))
       .andExpect(jsonPath("$.problemPatternList[0].recordAnalysisList[2].problemOccurrenceList[0].messageReport", is("")));
    verify(mockPatternAnalysisService, times(1)).finalizeDatasetPatternAnalysis(executionPoint);
  }

  @Test
  void getRecordPatternAnalysis_withCleanMessageReportP7AndSorted_expectSuccess() throws Exception {
    RecordLogEntity mockRecordLogEntity = mock(RecordLogEntity.class);
    List<ProblemPattern> problemPatternList = new ArrayList<>();
    RecordAnalysis recordAnalysis1 = new RecordAnalysis("recordId1", List.of(new ProblemOccurrence("text1", List.of())));
    problemPatternList.add(new ProblemPattern(ProblemPatternDescription.P7, 1,
        List.of(recordAnalysis1)));
    String recordContent = IOUtils.toString(
        new FileInputStream("src/test/resources/record.problempatterns/record_pattern_problem_with_P7.xml"),
        StandardCharsets.UTF_8);

    when(mockRecordLogService.getRecordLogEntity("recordId", "datasetId", Step.VALIDATE_INTERNAL)).thenReturn(
        mockRecordLogEntity);
    when(mockRecordLogEntity.getContent()).thenReturn(recordContent);
    when(mockPatternAnalysisService.getRecordPatternAnalysis(any(RDF.class)))
        .thenReturn(problemPatternList);

    mvc.perform(get("/pattern-analysis/{id}/get-record-pattern-analysis", "datasetId")
           .param("recordId", "recordId"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$[0].problemPatternDescription.problemPatternId", is("P7")))
       .andExpect(jsonPath("$[0].problemPatternDescription.problemPatternTitle", is("Missing description fields")))
       .andExpect(jsonPath("$[0].problemPatternDescription.problemPatternSeverity", is("WARNING")))
       .andExpect(jsonPath("$[0].problemPatternDescription.problemPatternQualityDimension", is("COMPLETENESS")))
       .andExpect(jsonPath("$[0].recordOccurrences", is(1)))
       .andExpect(jsonPath("$[0].recordAnalysisList[0].recordId", is("recordId1")))
       .andExpect(jsonPath("$[0].recordAnalysisList[0].problemOccurrenceList[0].messageReport", is("")));
  }
}
