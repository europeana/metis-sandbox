package eu.europeana.metis.sandbox.common.batch;

/**
 * Represents the different types of batch jobs supported by the application.
 */
public enum BatchJobType {
  HARVEST_OAI,
  HARVEST_FILE,
  VALIDATE,
  TRANSFORM,
  NORMALIZE,
  ENRICH,
  MEDIA,
  INDEX,
  DEBIAS
}
