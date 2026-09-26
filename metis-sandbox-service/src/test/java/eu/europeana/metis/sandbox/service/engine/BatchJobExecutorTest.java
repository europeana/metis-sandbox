package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.common.batch.FullBatchJobType.NORMALIZE;
import static eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey.ENGINE_DATASET_ID;
import static eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey.JOB_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRunRepository;
import eu.europeana.metis.sandbox.common.task.input.HttpHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskRequest;
import eu.europeana.metis.sandbox.common.task.input.SimpleIntermediateInputMetadataRequest;
import eu.europeana.metis.sandbox.dto.ExecutionMetadata;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.TaskExecutor;

@ExtendWith(MockitoExtension.class)
class BatchJobExecutorTest {

  @Mock
  private JobOperator jobOperator;

  @Mock
  private JobRepository jobRepository;

  @Mock
  private TaskExecutor taskExecutor;

  @Mock
  private TransformXsltRepository transformXsltRepository;

  @Mock
  private ExecutionRunRepository executionRunRepository;

  @Mock
  private ExecutionMetadata executionMetadata;

  private BatchJobExecutor batchJobExecutor;

  @BeforeEach
  void setUp() {
    batchJobExecutor = new BatchJobExecutor(
        List.of(),
        jobOperator,
        jobRepository,
        taskExecutor,
        transformXsltRepository,
        executionRunRepository);
  }

  @Test
  void createTaskPersistsExecutionRunBeforeReturningIt() {
    returnSavedExecutionRun();
    SandboxTaskRequest request = taskRequest(new HttpHarvestInputMetadataRequest("https://example.org/records.zip", 10));

    ExecutionRun executionRun = batchJobExecutor.createTask(request);

    assertEquals("42", executionRun.getDatasetId());
    assertEquals(NORMALIZE.name(), executionRun.getExecutionName());
    assertNull(executionRun.getSourceExecutionId());
    InOrder persistenceOrder = inOrder(executionRunRepository);
    persistenceOrder.verify(executionRunRepository).save(executionRun);
    verifyNoInteractions(taskExecutor);
  }

  @Test
  void createTaskStoresSourceExecutionIdForIntermediateInput() {
    returnSavedExecutionRun();
    SandboxTaskRequest request = taskRequest(new SimpleIntermediateInputMetadataRequest("source-execution-id"));
    ExecutionRun executionRun = batchJobExecutor.createTask(request);
    assertEquals("source-execution-id", executionRun.getSourceExecutionId());
  }

  @Test
  void executeCreatedTaskReusesPersistedExecutionId() {
    String executionId = UUID.randomUUID().toString();
    when(executionRunRepository.findByExecutionId(executionId)).thenReturn(new ExecutionRun());
    String result = batchJobExecutor.submitTask(executionMetadata, NORMALIZE, executionId);
    assertEquals(executionId, result);
    verify(executionRunRepository, never()).save(any(ExecutionRun.class));
    verify(taskExecutor).execute(any(Runnable.class));
  }

  private void returnSavedExecutionRun() {
    when(executionRunRepository.save(any(ExecutionRun.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private static SandboxTaskRequest taskRequest(
      eu.europeana.metis.sandbox.common.task.input.InputMetadataRequest inputMetadataRequest) {
    SandboxTaskRequest request = new SandboxTaskRequest();
    request.setParameters(Map.of(
        ENGINE_DATASET_ID, "42",
        JOB_NAME, NORMALIZE.name()));
    request.setInputMetadataRequest(inputMetadataRequest);
    return request;
  }
}
