package eu.europeana.metis.sandbox.controller.task;

import static com.google.common.base.Preconditions.checkArgument;
import static eu.europeana.metis.sandbox.controller.DatasetHarvestController.INVALID_STEP_SIZE_MESSAGE;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTask;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTaskKey;
import eu.europeana.metis.sandbox.controller.task.input.InputMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.InternalInputMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.OaiHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTaskProgress;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionService;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import java.io.IOException;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/")
public class TaskController {

  private final DatasetExecutionSetupService datasetExecutionSetupService;
  private final DatasetExecutionService datasetExecutionService;
  private final DatasetReportService datasetReportService;
  private final UrlValidator urlValidator;

  public TaskController(DatasetExecutionSetupService datasetExecutionSetupService,
      DatasetExecutionService datasetExecutionService, DatasetReportService datasetReportService, UrlValidator urlValidator) {
    this.datasetExecutionSetupService = datasetExecutionSetupService;
    this.datasetExecutionService = datasetExecutionService;
    this.datasetReportService = datasetReportService;
    this.urlValidator = urlValidator;
  }

  @PostMapping("/dataset")
  public String createEngineDataset(@RequestBody DatasetMetadataRequest datasetMetadataRequest) {
    return datasetExecutionSetupService.createDataset(datasetMetadataRequest, WorkflowType.SINGLE, "ENGINE");
  }

  @PostMapping("/submit")
  public long submitTask(@RequestBody SandboxTask sandboxTask) throws IOException {
    int stepSize = Integer.valueOf(sandboxTask.getParameters().get(SandboxTaskKey.STEP_SIZE));
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
      case InternalInputMetadataRequest internalInputMetadataRequest ->
          datasetExecutionService.submitExecutionSingle(datasetId, internalInputMetadataRequest.sourceExecutionId(), null,
              FullBatchJobType.valueOf(jobName));
    };
  }

  @GetMapping("/progress")
  public SandboxTaskProgress taskProgress(
      @RequestParam(name = "jobExecutionId") long jobExecutionId,
      @RequestParam(name = "datasetId") String datasetId,
      @RequestParam(name = "step") FullBatchJobType step) {
    return datasetReportService.getProgressForStep(jobExecutionId, datasetId, step);
  }

  @PostMapping("/cancel")
  public String cancelTask(@RequestParam(name = "param1") String param1, @RequestParam(name = "param2") String param2) {
    //todo: update what input and output
    return "Task requested with parameters: " + param1 + ", " + param2;
  }

}
