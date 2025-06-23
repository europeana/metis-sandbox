package eu.europeana.metis.sandbox.entity;

/**
 * Represents the type of workflow.
 *
 * <p>Defines the various types of workflows, including harvesting and validation procedures.
 * <p>The special type {@link WorkflowType#DEBIAS} is not stored in the database and is only used to trigger specific steps.
 * <p>Note: This is a temporary implementation for workflow handling in sandbox.
 */
public enum WorkflowType {
  OAI_HARVEST, FILE_HARVEST, FILE_HARVEST_ONLY_VALIDATION, OLD_HARVEST,
  DEBIAS
}
