package eu.europeana.metis.sandbox.controller.task;

import static eu.europeana.metis.sandbox.common.batch.FullBatchJobType.NORMALIZE;
import static eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey.ENGINE_DATASET_ID;
import static eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey.JOB_NAME;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.indexing.IndexerPool;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskRequest;
import eu.europeana.metis.sandbox.common.task.input.SimpleIntermediateInputMetadataRequest;
import eu.europeana.metis.sandbox.config.SecurityConfig;
import eu.europeana.metis.sandbox.config.webmvc.WebMvcConfig;
import eu.europeana.metis.sandbox.controller.advice.RestResponseExceptionHandler;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@ContextConfiguration(classes = {WebMvcConfig.class, RestResponseExceptionHandler.class, SecurityConfig.class,
    TaskController.class})
class TaskControllerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String DATASET_ID = "datasetId";
  private static final String SOURCE_EXECUTION_ID = "sourceExecutionId";

  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private DatasetExecutionSetupService datasetExecutionSetupService;

  @MockitoBean
  private DatasetExecutionService datasetExecutionService;

  @MockitoBean
  private DatasetReportService datasetReportService;

  @MockitoBean
  private IndexerPool indexerPool;

  @Autowired
  private TaskController taskController;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void createTaskEndpointReturnsCreatedTask() throws Exception {
    String executionId = UUID.randomUUID().toString();
    SandboxTaskRequest request = taskRequest();
    when(datasetExecutionService.createTask(any(SandboxTaskRequest.class)))
        .thenReturn(new SandboxTask(request, executionId, executionId));

    mockMvc.perform(post("/task/create")
               .contentType(MediaType.APPLICATION_JSON)
               .content(OBJECT_MAPPER.writeValueAsString(request)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.taskId", is(executionId)))
           .andExpect(jsonPath("$.batchId", is(executionId)))
           .andExpect(jsonPath("$.parameters.ENGINE_DATASET_ID", is(DATASET_ID)))
           .andExpect(jsonPath("$.inputMetadataRequest.sourceExecutionId", is(SOURCE_EXECUTION_ID)));
  }

  @Test
  void createTaskDelegatesToDatasetExecutionService() {
    SandboxTaskRequest request = taskRequest();
    String executionId = UUID.randomUUID().toString();
    SandboxTask expectedTask = new SandboxTask(request, executionId, executionId);
    when(datasetExecutionService.createTask(request)).thenReturn(expectedTask);

    SandboxTask sandboxTask = taskController.createTask(request);

    assertSame(expectedTask, sandboxTask);
  }

  @Test
  void submitTaskUsesIdentifierAssignedDuringTaskCreation() throws Exception {
    String executionId = UUID.randomUUID().toString();
    SandboxTask sandboxTask = new SandboxTask(taskRequest(), executionId, executionId);

    mockMvc.perform(post("/task/submit")
               .contentType(MediaType.APPLICATION_JSON)
               .content(OBJECT_MAPPER.writeValueAsString(sandboxTask)))
           .andExpect(status().isAccepted());

    verify(datasetExecutionService).submitIntermediateExecutionSingle(executionId, DATASET_ID, SOURCE_EXECUTION_ID, NORMALIZE);
  }

  private static SandboxTaskRequest taskRequest() {
    SandboxTaskRequest request = new SandboxTaskRequest();
    request.setParameters(Map.of(
        ENGINE_DATASET_ID, DATASET_ID,
        JOB_NAME, NORMALIZE.name()));
    request.setInputMetadataRequest(new SimpleIntermediateInputMetadataRequest(SOURCE_EXECUTION_ID));
    return request;
  }
}
