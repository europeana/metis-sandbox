package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.common.batch.ArgumentString.ARGUMENT_DATASET_ID;
import static eu.europeana.metis.sandbox.common.batch.ArgumentString.ARGUMENT_BATCH_JOB_SUBTYPE;
import static eu.europeana.metis.sandbox.common.batch.ArgumentString.ARGUMENT_SOURCE_EXECUTION_ID;
import static eu.europeana.metis.sandbox.common.batch.ArgumentString.ARGUMENT_TARGET_EXECUTION_ID;
import static eu.europeana.metis.sandbox.common.batch.FullBatchJobType.ENRICH;
import static eu.europeana.metis.sandbox.common.batch.FullBatchJobType.NORMALIZE;
import static eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey.ENGINE_DATASET_ID;
import static eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey.JOB_NAME;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRunRepository;
import eu.europeana.metis.sandbox.common.WorkflowType;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.common.task.input.HttpHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.InputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskRequest;
import eu.europeana.metis.sandbox.common.task.input.SimpleIntermediateInputMetadataRequest;
import eu.europeana.metis.sandbox.dto.DatasetMetadata;
import eu.europeana.metis.sandbox.dto.ExecutionMetadata;
import eu.europeana.metis.sandbox.dto.InputMetadata;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.support.locks.LockRegistry;

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

  @Mock
  private JobOperator engineJobOperator;

  @Mock
  private LockRegistry<DistributedLock> lockRegistry;

  private Job normalizeJob;
  private Job enrichJob;
  private BatchJobExecutor batchJobExecutor;
  private final TestDistributedLock submissionLock = new TestDistributedLock();
  private static final String DATASET_ID = "42";
  private static final String SOURCE_ID = "sourceExecutionId";
  private static final String EXECUTION_ID = "executionId";

  @BeforeEach
  void setUp() {
    normalizeJob = mock(Job.class);
    enrichJob = mock(Job.class);
    when(normalizeJob.getName()).thenReturn("NORMALIZE");
    when(enrichJob.getName()).thenReturn("ENRICH");
    batchJobExecutor = new BatchJobExecutor(
        List.of(normalizeJob, enrichJob),
        jobOperator,
        jobRepository,
        taskExecutor,
        transformXsltRepository,
        executionRunRepository, engineJobOperator, lockRegistry);
  }

  @Test
  void createTaskPersistsExecutionRunBeforeReturningIt() {
    returnSavedExecutionRun();
    SandboxTaskRequest sandboxTaskRequest = taskRequest(new HttpHarvestInputMetadataRequest("https://example.org/records.zip", 10));

    ExecutionRun executionRun = batchJobExecutor.createTask(sandboxTaskRequest);

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
    SandboxTaskRequest sandboxTaskRequest = taskRequest(new SimpleIntermediateInputMetadataRequest("sourceExecutionId"));
    ExecutionRun executionRun = batchJobExecutor.createTask(sandboxTaskRequest);
    assertEquals("sourceExecutionId", executionRun.getSourceExecutionId());
  }

  @Test
  void submitsCreatedTaskThroughEngineOperatorWithStableJobIdentity() throws Exception {
    prepareCreatedTask();
    ExecutionMetadata preparedMetadata = intermediateMetadata(WorkflowType.SINGLE);
    Supplier<ExecutionMetadata> metadataSupplier = mock(Supplier.class);
    when(metadataSupplier.get()).thenReturn(preparedMetadata);

    batchJobExecutor.submitTask(EXECUTION_ID, DATASET_ID, SOURCE_ID, NORMALIZE, metadataSupplier);

    InOrder submissionOrder = inOrder(metadataSupplier, engineJobOperator);
    submissionOrder.verify(metadataSupplier).get();
    submissionOrder.verify(engineJobOperator).start(eq(normalizeJob), any(JobParameters.class));
    ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(engineJobOperator).start(eq(normalizeJob), parametersCaptor.capture());
    JobParameters jobParameters = parametersCaptor.getValue();
    assertEquals(EXECUTION_ID, jobParameters.getString(ARGUMENT_TARGET_EXECUTION_ID));
    assertEquals(DATASET_ID, jobParameters.getString(ARGUMENT_DATASET_ID));
    assertOnlyTargetExecutionIdIsIdentifying(jobParameters);
    verifyNoInteractions(taskExecutor, jobOperator);
  }

  @ParameterizedTest
  @EnumSource(BatchStatus.class)
  void repeatedSubmissionAcknowledgesAnyExistingExecutionWithoutPreparation(BatchStatus batchStatus) {
    prepareCreatedTask();
    JobInstance jobInstance = new JobInstance(1L, "NORMALIZE");
    JobExecution jobExecution = new JobExecution(1L, jobInstance, identifyingParameters(EXECUTION_ID));
    jobExecution.setStatus(batchStatus);
    when(jobRepository.findJobInstances("NORMALIZE")).thenReturn(List.of(jobInstance));
    when(jobRepository.getJobExecutions(jobInstance)).thenReturn(List.of(jobExecution));
    Supplier<ExecutionMetadata> metadataSupplier = mock(Supplier.class);

    batchJobExecutor.submitTask(EXECUTION_ID, DATASET_ID, SOURCE_ID, NORMALIZE, metadataSupplier);

    verifyNoInteractions(metadataSupplier, engineJobOperator, taskExecutor, jobOperator);
    assertFalse(submissionLock.isLocked());
  }

  @Test
  void rejectsMissingOrMismatchedTaskBeforePreparingMetadata() {
    when(lockRegistry.obtain("engineTask_" + EXECUTION_ID)).thenReturn(submissionLock);
    Supplier<ExecutionMetadata> metadataSupplier = mock(Supplier.class);
    assertThrows(IllegalArgumentException.class, () ->
        batchJobExecutor.submitTask(EXECUTION_ID, DATASET_ID, SOURCE_ID, NORMALIZE, metadataSupplier));
    ExecutionRun executionRun = createdExecutionRun();
    when(executionRunRepository.findByExecutionId(EXECUTION_ID)).thenReturn(executionRun);
    assertThrows(IllegalArgumentException.class, () ->
        batchJobExecutor.submitTask(EXECUTION_ID, "otherDataset", SOURCE_ID, NORMALIZE, metadataSupplier));
    assertThrows(IllegalArgumentException.class, () ->
        batchJobExecutor.submitTask(EXECUTION_ID, DATASET_ID, "otherSource", NORMALIZE, metadataSupplier));
    assertThrows(IllegalArgumentException.class, () ->
        batchJobExecutor.submitTask(EXECUTION_ID, DATASET_ID, SOURCE_ID, ENRICH, metadataSupplier));
    verifyNoInteractions(metadataSupplier, engineJobOperator, taskExecutor, jobOperator);
    assertFalse(submissionLock.isLocked());
  }

  @Test
  void concurrentSubmissionsPrepareAndLaunchOnlyOnceAcrossExecutorInstances() throws Exception {
    prepareCreatedTask();
    BatchJobExecutor secondExecutor = new BatchJobExecutor(List.of(normalizeJob), jobOperator, jobRepository,
        taskExecutor, transformXsltRepository, executionRunRepository, engineJobOperator, lockRegistry);
    List<JobExecution> registeredExecutions = new ArrayList<>();
    JobInstance jobInstance = new JobInstance(1L, "NORMALIZE");
    when(jobRepository.findJobInstances("NORMALIZE")).thenReturn(List.of(jobInstance));
    when(jobRepository.getJobExecutions(jobInstance)).thenAnswer(invocation -> List.copyOf(registeredExecutions));
    CountDownLatch preparationStarted = new CountDownLatch(1);
    CountDownLatch allowPreparation = new CountDownLatch(1);
    Supplier<ExecutionMetadata> metadataSupplier = mock(Supplier.class);
    when(metadataSupplier.get()).thenAnswer(invocation -> {
      preparationStarted.countDown();
      assertTrue(allowPreparation.await(5, TimeUnit.SECONDS));
      return intermediateMetadata(WorkflowType.SINGLE);
    });
    when(engineJobOperator.start(eq(normalizeJob), any(JobParameters.class))).thenAnswer(invocation -> {
      JobExecution jobExecution = new JobExecution(1L, jobInstance, invocation.getArgument(1));
      registeredExecutions.add(jobExecution);
      return jobExecution;
    });
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      Future<?> firstSubmission = executorService.submit(() -> batchJobExecutor.submitTask(
          EXECUTION_ID, DATASET_ID, SOURCE_ID, NORMALIZE, metadataSupplier));
      assertTrue(preparationStarted.await(5, TimeUnit.SECONDS));
      Future<?> secondSubmission = executorService.submit(() -> secondExecutor.submitTask(
          EXECUTION_ID, DATASET_ID, SOURCE_ID, NORMALIZE, metadataSupplier));
      allowPreparation.countDown();
      firstSubmission.get(5, TimeUnit.SECONDS);
      secondSubmission.get(5, TimeUnit.SECONDS);
    } finally {
      allowPreparation.countDown();
    }
    verify(metadataSupplier).get();
    verify(engineJobOperator).start(eq(normalizeJob), any(JobParameters.class));
    verifyNoInteractions(taskExecutor, jobOperator);
  }

  @Test
  void uiManagedDebiasWorkflowStillUsesSynchronousOperatorAndFreshExecutionIds() throws Exception {
    Job debiasJob = mock(Job.class);
    when(debiasJob.getName()).thenReturn("DEBIAS");
    BatchJobExecutor debiasWorkflowExecutor = new BatchJobExecutor(List.of(debiasJob), jobOperator, jobRepository,
        taskExecutor, transformXsltRepository, executionRunRepository, engineJobOperator, lockRegistry);
    ExecutionMetadata preparedMetadata = intermediateMetadata(WorkflowType.DEBIAS);
    when(executionRunRepository.save(any(ExecutionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jobOperator.start(eq(debiasJob), any(JobParameters.class))).thenAnswer(invocation -> {
      JobExecution jobExecution = new JobExecution(1L, new JobInstance(1L, "DEBIAS"), invocation.getArgument(1));
      jobExecution.setStatus(BatchStatus.COMPLETED);
      return jobExecution;
    });

    debiasWorkflowExecutor.executeBlocking(preparedMetadata);
    debiasWorkflowExecutor.executeBlocking(preparedMetadata);

    ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator, times(2)).start(eq(debiasJob), parametersCaptor.capture());
    List<JobParameters> parameters = parametersCaptor.getAllValues();
    assertNotEquals(parameters.getFirst().getString(ARGUMENT_TARGET_EXECUTION_ID),
        parameters.getLast().getString(ARGUMENT_TARGET_EXECUTION_ID));
    parameters.forEach(BatchJobExecutorTest::assertOnlyTargetExecutionIdIsIdentifying);
    verifyNoInteractions(engineJobOperator, lockRegistry);
  }

  @Test
  void uiManagedHarvestWorkflowStillPassesEachStepsOutputToTheNext() throws Exception {
    Job harvestJob = mock(Job.class);
    Job validationJob = mock(Job.class);
    Job transformationJob = mock(Job.class);
    when(harvestJob.getName()).thenReturn("HARVEST_FILE");
    when(validationJob.getName()).thenReturn("VALIDATE");
    when(transformationJob.getName()).thenReturn("TRANSFORM");
    Job mediaJob = mock(Job.class);
    Job indexJob = mock(Job.class);
    when(mediaJob.getName()).thenReturn("MEDIA");
    when(indexJob.getName()).thenReturn("INDEX");
    BatchJobExecutor workflowExecutor = new BatchJobExecutor(
        List.of(harvestJob, validationJob, transformationJob, normalizeJob, enrichJob, mediaJob, indexJob),
        jobOperator, jobRepository, taskExecutor, transformXsltRepository, executionRunRepository, engineJobOperator, lockRegistry);
    HarvestParametersEntity harvestParameters = new HarvestParametersEntity();
    harvestParameters.setId(UUID.randomUUID());
    harvestParameters.setStepSize(10);
    TransformXsltEntity transformXslt = new TransformXsltEntity();
    transformXslt.setId(1);
    when(transformXsltRepository.findByDatasetIdAndType(DATASET_ID, XsltType.INTERNAL)).thenReturn(Optional.of(transformXslt));
    ExecutionMetadata workflowMetadata = ExecutionMetadata.builder()
        .datasetMetadata(datasetMetadata(WorkflowType.FILE_HARVEST))
        .inputMetadata(new InputMetadata(harvestParameters, null)).build();
    when(executionRunRepository.save(any(ExecutionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenAnswer(invocation -> {
      Job executionJob = invocation.getArgument(0);
      JobExecution jobExecution = new JobExecution(1L, new JobInstance(1L, executionJob.getName()), invocation.getArgument(1));
      jobExecution.setStatus(BatchStatus.COMPLETED);
      return jobExecution;
    });

    workflowExecutor.executeBlocking(workflowMetadata);

    ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    InOrder workflowOrder = inOrder(jobOperator);
    workflowOrder.verify(jobOperator).start(eq(harvestJob), parametersCaptor.capture());
    workflowOrder.verify(jobOperator).start(eq(validationJob), parametersCaptor.capture());
    workflowOrder.verify(jobOperator).start(eq(transformationJob), parametersCaptor.capture());
    workflowOrder.verify(jobOperator).start(eq(validationJob), parametersCaptor.capture());
    workflowOrder.verify(jobOperator).start(eq(normalizeJob), parametersCaptor.capture());
    workflowOrder.verify(jobOperator).start(eq(enrichJob), parametersCaptor.capture());
    workflowOrder.verify(jobOperator).start(eq(mediaJob), parametersCaptor.capture());
    workflowOrder.verify(jobOperator).start(eq(indexJob), parametersCaptor.capture());
    List<JobParameters> stepParameters = parametersCaptor.getAllValues();
    stepParameters.forEach(BatchJobExecutorTest::assertOnlyTargetExecutionIdIsIdentifying);
    for (int step = 1; step < stepParameters.size(); step++) {
      assertEquals(stepParameters.get(step - 1).getString(ARGUMENT_TARGET_EXECUTION_ID),
          stepParameters.get(step).getString(ARGUMENT_SOURCE_EXECUTION_ID));
    }
    verify(executionRunRepository, times(8)).save(any(ExecutionRun.class));
    verifyNoInteractions(engineJobOperator, lockRegistry);
  }

  @Test
  void uiManagedAsynchronousWorkflowStillUsesPipelineExecutor() {
    batchJobExecutor.executeWorkflow(executionMetadata);
    verify(taskExecutor).execute(any(Runnable.class));
    verifyNoInteractions(engineJobOperator, lockRegistry);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void historicalExecutionLookupAndCancellationAcceptBothIdentityFormats(boolean legacyIdentity) throws Exception {
    JobInstance jobInstance = new JobInstance(1L, "NORMALIZE");
    JobParameters storedParameters = new JobParametersBuilder()
        .addString(ARGUMENT_TARGET_EXECUTION_ID, EXECUTION_ID)
        .addString(ARGUMENT_DATASET_ID, DATASET_ID, legacyIdentity)
        .addString(ARGUMENT_SOURCE_EXECUTION_ID, SOURCE_ID, legacyIdentity).toJobParameters();
    JobExecution storedExecution = new JobExecution(1L, jobInstance, storedParameters);
    JobExecution otherExecution = new JobExecution(2L, jobInstance, identifyingParameters("otherExecutionId"));
    when(jobRepository.findJobInstances("NORMALIZE")).thenReturn(List.of(jobInstance));
    when(jobRepository.getJobExecutions(jobInstance)).thenReturn(List.of(otherExecution, storedExecution));

    assertSame(storedExecution, batchJobExecutor.findJobExecutionByParameter(EXECUTION_ID, NORMALIZE));
    batchJobExecutor.cancelTask(EXECUTION_ID, NORMALIZE);

    verify(jobOperator).stop(storedExecution);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void debiasFindsHistoricalInternalValidationWithEitherIdentityFormat(boolean legacyIdentity) throws Exception {
    Job debiasJob = mock(Job.class);
    when(debiasJob.getName()).thenReturn("DEBIAS");
    BatchJobExecutor debiasWorkflowExecutor = new BatchJobExecutor(List.of(debiasJob), jobOperator, jobRepository,
        taskExecutor, transformXsltRepository, executionRunRepository, engineJobOperator, lockRegistry);
    JobInstance validationJobInstance = new JobInstance(1L, "VALIDATE");
    JobParameters internalParameters = new JobParametersBuilder()
        .addString(ARGUMENT_TARGET_EXECUTION_ID, "validationExecutionId")
        .addString(ARGUMENT_DATASET_ID, DATASET_ID, legacyIdentity)
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, "INTERNAL", legacyIdentity).toJobParameters();
    JobParameters externalParameters = new JobParametersBuilder(internalParameters)
        .addString(ARGUMENT_TARGET_EXECUTION_ID, "externalValidationExecutionId")
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, "EXTERNAL", legacyIdentity).toJobParameters();
    when(jobRepository.getJobInstances("VALIDATE", 0, BatchJobExecutor.JOB_INSTANCE_RETRIEVAL_PAGE_SIZE))
        .thenReturn(List.of(validationJobInstance));
    when(jobRepository.getJobExecutions(validationJobInstance)).thenReturn(List.of(
        new JobExecution(1L, validationJobInstance, internalParameters),
        new JobExecution(2L, validationJobInstance, externalParameters)));
    when(executionRunRepository.save(any(ExecutionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jobOperator.start(eq(debiasJob), any(JobParameters.class))).thenAnswer(invocation -> {
      JobExecution jobExecution = new JobExecution(3L, new JobInstance(2L, "DEBIAS"), invocation.getArgument(1));
      jobExecution.setStatus(BatchStatus.COMPLETED);
      return jobExecution;
    });

    debiasWorkflowExecutor.executeDebiasWorkflow(intermediateMetadata(WorkflowType.DEBIAS));
    ArgumentCaptor<Runnable> workflowCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(taskExecutor).execute(workflowCaptor.capture());
    workflowCaptor.getValue().run();

    ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator).start(eq(debiasJob), parametersCaptor.capture());
    assertEquals("validationExecutionId", parametersCaptor.getValue().getString(ARGUMENT_SOURCE_EXECUTION_ID));
    assertOnlyTargetExecutionIdIsIdentifying(parametersCaptor.getValue());
    verifyNoInteractions(engineJobOperator, lockRegistry);
  }

  private static void assertOnlyTargetExecutionIdIsIdentifying(JobParameters jobParameters) {
    assertEquals(1, jobParameters.getIdentifyingParameters().size());
    assertTrue(requireNonNull(jobParameters.getParameter(ARGUMENT_TARGET_EXECUTION_ID)).identifying());
    for (JobParameter<?> jobParameter : jobParameters) {
      assertEquals(ARGUMENT_TARGET_EXECUTION_ID.equals(jobParameter.name()), jobParameter.identifying());
    }
  }

  private void prepareCreatedTask() {
    when(lockRegistry.obtain("engineTask_" + EXECUTION_ID)).thenReturn(submissionLock);
    when(executionRunRepository.findByExecutionId(EXECUTION_ID)).thenReturn(createdExecutionRun());
  }

  private static ExecutionRun createdExecutionRun() {
    ExecutionRun executionRun = new ExecutionRun();
    executionRun.setExecutionId(EXECUTION_ID);
    executionRun.setDatasetId(DATASET_ID);
    executionRun.setExecutionName(NORMALIZE.name());
    executionRun.setSourceExecutionId(SOURCE_ID);
    return executionRun;
  }

  private static JobParameters identifyingParameters(String executionId) {
    return new JobParametersBuilder().addString(ARGUMENT_TARGET_EXECUTION_ID, executionId).toJobParameters();
  }

  private static DatasetMetadata datasetMetadata(WorkflowType workflowType) {
    return DatasetMetadata.builder()
        .datasetId(DATASET_ID)
        .datasetName("datasetName")
        .country(Country.NETHERLANDS)
        .language(Language.EN)
        .workflowType(workflowType)
        .build();
  }

  private static ExecutionMetadata intermediateMetadata(WorkflowType workflowType) {
    return ExecutionMetadata.builder()
        .datasetMetadata(datasetMetadata(workflowType))
        .inputMetadata(new InputMetadata(SOURCE_ID))
        .build();
  }

  private static class TestDistributedLock extends ReentrantLock implements DistributedLock {
    @Override
    public void lock(Duration leaseTime) {
      lock();
    }

    @Override
    public boolean tryLock(Duration waitTime, Duration leaseTime) throws InterruptedException {
      return tryLock(waitTime.toNanos(), TimeUnit.NANOSECONDS);
    }
  }

  private void returnSavedExecutionRun() {
    when(executionRunRepository.save(any(ExecutionRun.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private static SandboxTaskRequest taskRequest(InputMetadataRequest inputMetadataRequest) {
    SandboxTaskRequest sandboxTaskRequest = new SandboxTaskRequest();
    sandboxTaskRequest.setParameters(Map.of(
        ENGINE_DATASET_ID, "42",
        JOB_NAME, NORMALIZE.name()));
    sandboxTaskRequest.setInputMetadataRequest(inputMetadataRequest);
    return sandboxTaskRequest;
  }
}
