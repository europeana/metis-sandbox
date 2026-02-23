package eu.europeana.metis.sandbox.controller.task;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.europeana.metis.sandbox.controller.DatasetHarvestController.INVALID_STEP_SIZE_MESSAGE;

import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.InputMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.InternalInputMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.OaiHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTask;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTaskKey;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTaskProgress;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.io.IOException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/")
@AllArgsConstructor
public class TaskController {

  private final DatasetExecutionSetupService datasetExecutionSetupService;
  private final DatasetExecutionService datasetExecutionService;
  private final DatasetReportService datasetReportService;
  private final Indexer<FullBeanImpl> publishIndexer;

  @PostMapping("/dataset")
  public String createEngineDataset(@RequestBody DatasetMetadataRequest datasetMetadataRequest) {
    return datasetExecutionSetupService.createDataset(datasetMetadataRequest, WorkflowType.SINGLE, "ENGINE");
  }

  @PostMapping("/submit")
  public String submitTask(@RequestBody SandboxTask sandboxTask) throws IOException {
    int stepSize = Optional.ofNullable(sandboxTask.getParameters().get(SandboxTaskKey.STEP_SIZE))
                           .map(Integer::parseInt)
                           .orElse(1);
    String datasetId = sandboxTask.getParameters().get(SandboxTaskKey.ENGINE_DATASET_ID);
    String jobName = sandboxTask.getParameters().get(SandboxTaskKey.JOB_NAME);

    checkArgument(stepSize > 0, INVALID_STEP_SIZE_MESSAGE);

    InputMetadataRequest inputMetadataRequest = sandboxTask.getInputMetadataRequest();
    return switch (inputMetadataRequest) {
      case OaiHarvestInputMetadataRequest oaiHarvestInputMetadataRequest -> datasetExecutionService.submitExecutionOaiSingle(
          datasetId, stepSize,
          oaiHarvestInputMetadataRequest.url(),
          oaiHarvestInputMetadataRequest.set(),
          oaiHarvestInputMetadataRequest.metadataPrefix());
      case InternalInputMetadataRequest internalInputMetadataRequest -> {
        String sourceExecutionId = internalInputMetadataRequest.sourceExecutionId();
        checkArgument(StringUtils.isNotBlank(sourceExecutionId), "Source execution ID cannot be blank.");
        yield datasetExecutionService.submitExecutionSingle(datasetId, internalInputMetadataRequest.sourceExecutionId(), null,
            FullBatchJobType.valueOf(jobName));
      }
    };
  }

  @GetMapping("/progress")
  public SandboxTaskProgress taskProgress(
      @RequestParam(name = "executionId") String executionId,
      @RequestParam(name = "datasetId") String datasetId,
      @RequestParam(name = "step") FullBatchJobType step) {
    return datasetReportService.getProgressForStep(executionId, datasetId, step);
  }

  @PostMapping("/cancel")
  public void cancelTask(
      @RequestParam(name = "executionId") String executionId,
      @RequestParam(name = "step") FullBatchJobType step) throws JobExecutionNotRunningException {
    datasetExecutionService.cancelTask(executionId, step);
  }

  @GetMapping("/indexedRecordsCount")
  public long getIndexedRecordsCount(@RequestParam(name = "metisDatasetId") String metisDatasetId) throws IndexingException {
    return publishIndexer.countRecords(metisDatasetId);
  }
}
