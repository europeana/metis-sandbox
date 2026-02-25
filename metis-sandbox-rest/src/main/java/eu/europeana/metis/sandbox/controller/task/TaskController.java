package eu.europeana.metis.sandbox.controller.task;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.europeana.metis.sandbox.controller.DatasetHarvestController.INVALID_STEP_SIZE_MESSAGE;

import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.metis.sandbox.common.FileTypeResolver;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.HttpHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.InputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.InternalInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.OaiHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskProgress;
import eu.europeana.metis.sandbox.common.WorkflowType;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
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
      case OaiHarvestInputMetadataRequest(String url, String set, String metadataPrefix, Date from, Date until) ->
          datasetExecutionService.submitExecutionOaiSingle(datasetId, stepSize, url, set, metadataPrefix);
      case HttpHarvestInputMetadataRequest(String url) -> {
        CompressedFileExtension compressedFileExtension = FileTypeResolver.fromUrl(URI.create(url));
        yield datasetExecutionService.submitExecutionHttpSingle(datasetId, stepSize, url, compressedFileExtension);
      }
      case InternalInputMetadataRequest(String sourceExecutionId) -> {
        checkArgument(StringUtils.isNotBlank(sourceExecutionId), "Source execution ID cannot be blank.");
        yield datasetExecutionService.submitIntermediateExecutionSingle(datasetId, sourceExecutionId, null,
            FullBatchJobType.valueOf(jobName));
      }
    };
  }

  @GetMapping("/progress")
  public SandboxTaskProgress taskProgress(
      @RequestParam(name = "executionId") String executionId,
      @RequestParam(name = "step") FullBatchJobType step) {
    return datasetReportService.getProgressForStep(executionId, step);
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
