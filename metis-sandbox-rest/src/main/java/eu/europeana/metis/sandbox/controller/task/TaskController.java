package eu.europeana.metis.sandbox.controller.task;

import static com.google.common.base.Preconditions.checkArgument;

import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.FileTypeResolver;
import eu.europeana.metis.sandbox.common.WorkflowType;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.sandbox.common.task.input.HttpHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.InputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.OaiHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskProgress;
import eu.europeana.metis.sandbox.common.task.input.SimpleIntermediateInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.TransformExternalInputMetadataRequest;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TaskController is a REST controller responsible for managing dataset-related operations and task execution workflows. It
 * provides endpoints for creating, submitting, monitoring, and canceling tasks, as well as fetching indexing statistics.
 * <p>
 * This controller accommodates functionality for integration with metis-core so that metis-sandbox eventually becomes an engine
 */
@RestController
@RequestMapping("/task/")
@AllArgsConstructor
public class TaskController {

  private static final String ENGINE_USER_ID = "ENGINE";
  private final DatasetExecutionSetupService datasetExecutionSetupService;
  private final DatasetExecutionService datasetExecutionService;
  private final DatasetReportService datasetReportService;
  private final Indexer<FullBeanImpl> publishIndexer;

  /**
   * Creates a new engine dataset based on the provided metadata request.
   * <p>
   * This method uses datasets based on the standalone implementation and could be simplified in the future if metis-sandbox is
   * fully converted to an engine.
   * <p>
   * Temporarily a hardcoded engine user id is used and the {@code WorkflowType} is set to {@code WorkflowType.SINGLE}. This will
   * change once the metis-sandbox becomes a pure engine.
   *
   * @param datasetMetadataRequest the metadata request containing dataset details such as name, country, and language
   * @return the unique identifier of the created dataset as a {@code String}
   */
  @PostMapping("/dataset")
  public String createEngineDataset(@RequestBody DatasetMetadataRequest datasetMetadataRequest) {
    return datasetExecutionSetupService.createDataset(datasetMetadataRequest, WorkflowType.SINGLE, ENGINE_USER_ID);
  }

  /**
   * Submits a task for execution based on the provided input metadata request.
   *
   * @param sandboxTask the task to be submitted, containing the necessary parameters and input metadata request details
   * @return a {@code String} representing the result of the task submission, such as a task execution identifier
   * @throws IOException if an input or output exception occurs during task submission
   */
  @PostMapping("/submit")
  public String submitTask(@RequestBody SandboxTask sandboxTask) throws IOException {
    String datasetId = sandboxTask.getParameters().get(SandboxTaskKey.ENGINE_DATASET_ID);
    String jobName = sandboxTask.getParameters().get(SandboxTaskKey.JOB_NAME);

    InputMetadataRequest inputMetadataRequest = sandboxTask.getInputMetadataRequest();
    return switch (inputMetadataRequest) {
      case OaiHarvestInputMetadataRequest(
          String url, String set, String metadataPrefix, Instant from, Instant until, Integer stepSize
      ) -> datasetExecutionService.submitExecutionOaiSingle(datasetId, stepSize, url, set, metadataPrefix);
      case HttpHarvestInputMetadataRequest(String url, Integer stepSize) -> {
        CompressedFileExtension compressedFileExtension = FileTypeResolver.fromUrl(URI.create(url));
        yield datasetExecutionService.submitExecutionHttpSingle(datasetId, stepSize, url, compressedFileExtension);
      }
      case SimpleIntermediateInputMetadataRequest(String sourceExecutionId) -> {
        checkArgument(StringUtils.isNotBlank(sourceExecutionId), "Source execution ID cannot be blank.");
        yield datasetExecutionService.submitIntermediateExecutionSingle(datasetId, sourceExecutionId, null,
            FullBatchJobType.valueOf(jobName));
      }
      case TransformExternalInputMetadataRequest(String xslt, String sourceExecutionId) -> {
        checkArgument(StringUtils.isNotBlank(sourceExecutionId), "Source execution ID cannot be blank.");
        yield datasetExecutionService.submitIntermediateExecutionSingle(datasetId, sourceExecutionId, xslt,
            FullBatchJobType.valueOf(jobName));
      }
    };
  }

  /**
   * Retrieves the progress of a specific task execution step.
   *
   * @param executionId the unique identifier of the task execution
   * @param fullBatchJobType the specific step of the task execution for which progress is to be fetched
   * @return an instance of {@code SandboxTaskProgress} containing progress details for the specified step
   */
  @GetMapping("/progress")
  public SandboxTaskProgress taskProgress(
      @RequestParam(name = "executionId") String executionId,
      @RequestParam(name = "fullBatchJobType") FullBatchJobType fullBatchJobType) {
    return datasetReportService.getProgressForJob(executionId, fullBatchJobType);
  }

  /**
   * Cancels the execution of a specific task based on the provided execution identifier and step name.
   *
   * @param executionId the unique identifier of the task execution to be cancelled
   * @param fullBatchJobType the specific step of the task execution to be cancelled, represented by {@code FullBatchJobType}
   * @throws JobExecutionNotRunningException if the task execution is not currently running
   */
  @PostMapping("/cancel")
  public void cancelTask(
      @RequestParam(name = "executionId") String executionId,
      @RequestParam(name = "fullBatchJobType") FullBatchJobType fullBatchJobType) throws JobExecutionNotRunningException {
    datasetExecutionService.cancelTask(executionId, fullBatchJobType);
  }

  /**
   * Retrieves the count of indexed records for a given dataset.
   *
   * @param metisDatasetId the unique identifier of the dataset for which the indexed record count is to be retrieved
   * @return the count of indexed records as a {@code long} value
   * @throws IndexingException if an error occurs during the retrieval of the indexed record count
   */
  @GetMapping("/indexedRecordsCount")
  public long getIndexedRecordsCount(@RequestParam(name = "metisDatasetId") String metisDatasetId) throws IndexingException {
    return publishIndexer.countRecords(metisDatasetId);
  }
}
