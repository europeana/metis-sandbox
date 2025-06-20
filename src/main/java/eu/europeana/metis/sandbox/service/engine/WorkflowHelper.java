package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.ENRICH;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.HARVEST_FILE;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.HARVEST_OAI;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.INDEX_PUBLISH;
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
import lombok.experimental.UtilityClass;

/**
 * Provides workflow-related utility functions for managing different types of workflows.
 *
 * <p>Handles the selection and composition of workflow steps based on metadata and workflow types.
 * <p>Supports conditional modifications of workflows such as adding external transformation steps.
 * <p>Designed for temporary use until integrated with a proper orchestrator like metis-core.
 */
@UtilityClass
public final class WorkflowHelper {

  private static final List<FullBatchJobType> COMMON_POST_HARVEST =
      List.of(VALIDATE_EXTERNAL, TRANSFORM_INTERNAL, VALIDATE_INTERNAL, NORMALIZE, ENRICH, MEDIA, INDEX_PUBLISH);

  private static final List<FullBatchJobType> ONLY_VALIDATION =
      List.of(VALIDATE_EXTERNAL, TRANSFORM_INTERNAL, VALIDATE_INTERNAL);

  private static final List<FullBatchJobType> ONLY_DEBIAS = List.of(FullBatchJobType.DEBIAS);

  private static final List<FullBatchJobType> OLD_HARVEST_WORKFLOW_DISPLAY =
      prepend(HARVEST_OAI, prepend(HARVEST_FILE, COMMON_POST_HARVEST));
  private static final List<FullBatchJobType> HARVEST_OAI_WORKFLOW = prepend(HARVEST_OAI, COMMON_POST_HARVEST);
  private static final List<FullBatchJobType> HARVEST_FILE_WORKFLOW = prepend(HARVEST_FILE, COMMON_POST_HARVEST);
  private static final List<FullBatchJobType> HARVEST_FILE_UNTIL_VALIDATION = prepend(HARVEST_FILE, ONLY_VALIDATION);

  private static final Map<WorkflowType, List<FullBatchJobType>> WORKFLOW_BY_WORKFLOW_TYPE = Map.of(
      WorkflowType.OAI_HARVEST, HARVEST_OAI_WORKFLOW,
      WorkflowType.FILE_HARVEST, HARVEST_FILE_WORKFLOW,
      WorkflowType.FILE_HARVEST_ONLY_VALIDATION, HARVEST_FILE_UNTIL_VALIDATION,
      WorkflowType.OLD_HARVEST, OLD_HARVEST_WORKFLOW_DISPLAY,
      WorkflowType.DEBIAS, ONLY_DEBIAS
  );

  private static List<FullBatchJobType> prepend(FullBatchJobType first, List<FullBatchJobType> rest) {
    List<FullBatchJobType> result = new ArrayList<>(rest.size() + 1);
    result.add(first);
    result.addAll(rest);
    return Collections.unmodifiableList(result);
  }

  /**
   * Determines and returns the workflow steps based on the provided execution metadata.
   *
   * <p>The method retrieves the workflow type from the dataset metadata within the executionMetadata.
   * <p>Based on the workflow type, it selects the corresponding base steps and conditionally
   * adds an external transformation step if required.
   *
   * @param executionMetadata contains metadata about the dataset and input for the execution process
   * @return a list of workflow steps as FullBatchJobType objects
   */
  public static List<FullBatchJobType> getWorkflow(ExecutionMetadata executionMetadata) {
    WorkflowType workflowType = Optional.of(executionMetadata.getDatasetMetadata().workflowType())
                                        .orElse(WorkflowType.OAI_HARVEST);
    List<FullBatchJobType> baseSteps = WORKFLOW_BY_WORKFLOW_TYPE.get(workflowType);

    boolean shouldInsertTransformExternal =
        executionMetadata.getInputMetadata().getTransformXsltEntity() != null &&
            workflowType != WorkflowType.FILE_HARVEST_ONLY_VALIDATION;

    return conditionallyAddTransformExternalStep(baseSteps, shouldInsertTransformExternal);
  }

  /**
   * Determines and returns the workflow steps based on the provided dataset and transform metadata.
   *
   * <p>The method evaluates the workflow type associated with the dataset and excludes unsupported types like DEBIAS(it has its
   * own report, and it's not part of the main workflows).
   * <p>It selects the corresponding workflow steps and conditionally includes an external transformation step if required.
   * <p>This mainly used for reporting and not for execution.
   *
   * @param datasetEntity the entity representing the dataset, including its workflow type
   * @param transformXsltEntity the entity representing an optional XSLT transformation for the dataset
   * @return a list of workflow steps as FullBatchJobType objects
   * @throws IllegalArgumentException if the workflow type is DEBIAS, which is not supported
   */
  public static List<FullBatchJobType> getWorkflow(DatasetEntity datasetEntity, TransformXsltEntity transformXsltEntity) {
    WorkflowType workflowType = Optional.ofNullable(datasetEntity.getWorkflowType()).orElse(WorkflowType.OAI_HARVEST);
    if (workflowType == WorkflowType.DEBIAS) {
      throw new IllegalArgumentException("Debias workflow not supported");
    }

    List<FullBatchJobType> baseSteps = WORKFLOW_BY_WORKFLOW_TYPE.get(workflowType);

    boolean shouldInsertTransformExternal = transformXsltEntity != null &&
        workflowType != WorkflowType.FILE_HARVEST_ONLY_VALIDATION;

    return conditionallyAddTransformExternalStep(baseSteps, shouldInsertTransformExternal);
  }

  /**
   * Conditionally adds a {@link FullBatchJobType#TRANSFORM_EXTERNAL} step to the input list of full batch job types.
   *
   * <p>The method checks whether the `shouldInsertTransformExternal` flag is true.
   * <p>If true, it adds a {@link FullBatchJobType#TRANSFORM_EXTERNAL} step directly before a
   * {@link FullBatchJobType#VALIDATE_EXTERNAL} step in the input list. 
   * <p>If the flag is false, the input list remains unchanged.
   *
   * @param baseSteps the original list of full batch job steps
   * @param shouldInsertTransformExternal flag indicating whether to add the `TRANSFORM_EXTERNAL` step
   * @return a list of full batch job types with the conditionally added step
   */
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
