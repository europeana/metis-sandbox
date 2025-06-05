package eu.europeana.metis.sandbox.entity;

public enum WorkflowType {
  OAI_HARVEST, FILE_HARVEST, FILE_HARVEST_ONLY_VALIDATION, OLD_HARVEST,
  //Special type that is not stored in db and is used only to trigger a particular step
  DEBIAS
}
