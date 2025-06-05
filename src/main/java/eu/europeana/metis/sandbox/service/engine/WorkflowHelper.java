package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.ENRICH;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.HARVEST_FILE;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.HARVEST_OAI;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.INDEX;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.MEDIA;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.NORMALIZE;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.TRANSFORM_EXTERNAL;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.TRANSFORM_INTERNAL;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.VALIDATE_EXTERNAL;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.VALIDATE_INTERNAL;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkflowHelper {

  //Those are all temporary until we have a proper orchestrator(e.g. metis-core)
  private static final List<FullBatchJobType> COMMON_POST_HARVEST =
      List.of(VALIDATE_EXTERNAL, TRANSFORM_INTERNAL, VALIDATE_INTERNAL, NORMALIZE, ENRICH, MEDIA, INDEX);

  private static final List<FullBatchJobType> ONLY_VALIDATION =
      List.of(VALIDATE_EXTERNAL, TRANSFORM_INTERNAL, VALIDATE_INTERNAL);

  private static final List<FullBatchJobType> ONLY_DEBIAS = List.of(FullBatchJobType.DEBIAS);

  private static final List<FullBatchJobType> OLD_HARVEST_WORKFLOW_DISPLAY =
      prepend(HARVEST_OAI, prepend(HARVEST_FILE, COMMON_POST_HARVEST));
  private static final List<FullBatchJobType> HARVEST_OAI_WORKFLOW = prepend(HARVEST_OAI, COMMON_POST_HARVEST);
  private static final List<FullBatchJobType> HARVEST_FILE_WORKFLOW = prepend(HARVEST_FILE, COMMON_POST_HARVEST);
  private static final List<FullBatchJobType> HARVEST_FILE_UNTIL_VALIDATION = prepend(HARVEST_FILE, ONLY_VALIDATION);

  private static List<FullBatchJobType> prepend(FullBatchJobType first, List<FullBatchJobType> rest) {
    List<FullBatchJobType> result = new ArrayList<>(rest.size() + 1);
    result.add(first);
    result.addAll(rest);
    return Collections.unmodifiableList(result);
  }

  public static final Map<WorkflowType, List<FullBatchJobType>> WORKFLOW_BY_WORKFLOW_TYPE = Map.of(
      WorkflowType.OAI_HARVEST, HARVEST_OAI_WORKFLOW,
      WorkflowType.FILE_HARVEST, HARVEST_FILE_WORKFLOW,
      WorkflowType.FILE_HARVEST_ONLY_VALIDATION, HARVEST_FILE_UNTIL_VALIDATION,
      WorkflowType.OLD_HARVEST, OLD_HARVEST_WORKFLOW_DISPLAY,
      WorkflowType.DEBIAS, ONLY_DEBIAS
  );

  public static List<FullBatchJobType> getWorkflow(ExecutionMetadata executionMetadata) {
    WorkflowType workflowType = Optional.of(executionMetadata.getDatasetMetadata().getWorkflowType())
                                        .orElse(WorkflowType.OAI_HARVEST);
    List<FullBatchJobType> baseSteps = WORKFLOW_BY_WORKFLOW_TYPE.get(workflowType);

    boolean shouldInsertTransformExternal =
        executionMetadata.getInputMetadata().getTransformXsltEntity() != null &&
            workflowType != WorkflowType.FILE_HARVEST_ONLY_VALIDATION;

    return conditionallyAddTransformExternalStep(baseSteps, shouldInsertTransformExternal);
  }

  public static List<FullBatchJobType> getWorkflow(DatasetEntity datasetEntity, TransformXsltEntity transformXsltEntity) {
    WorkflowType workflowType = Optional.ofNullable(datasetEntity.getWorkflowType()).orElse(WorkflowType.OAI_HARVEST);
    //In this case DEBIAS workflow is not valid since we don't provide progress info for it for the dataset, it has its own report
    if (workflowType == WorkflowType.DEBIAS) {
      throw new IllegalArgumentException("Debias workflow not supported");
    }

    List<FullBatchJobType> baseSteps = WORKFLOW_BY_WORKFLOW_TYPE.get(workflowType);

    boolean shouldInsertTransformExternal = transformXsltEntity != null &&
        workflowType != WorkflowType.FILE_HARVEST_ONLY_VALIDATION;

    return conditionallyAddTransformExternalStep(baseSteps, shouldInsertTransformExternal);
  }

  public static List<FullBatchJobType> conditionallyAddTransformExternalStep(List<FullBatchJobType> baseSteps,
      boolean shouldInsertTransformExternal) {
    List<FullBatchJobType> finalSteps;
    if (shouldInsertTransformExternal) {
      finalSteps = new ArrayList<>();
      for (FullBatchJobType step : baseSteps) {
        if (step == VALIDATE_EXTERNAL) {
          finalSteps.add(TRANSFORM_EXTERNAL);
        }
        finalSteps.add(step);
      }
    } else {
      finalSteps = baseSteps;
    }

    return finalSteps;
  }
}
