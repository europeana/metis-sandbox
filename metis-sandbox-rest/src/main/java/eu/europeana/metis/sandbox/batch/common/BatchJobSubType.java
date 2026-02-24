package eu.europeana.metis.sandbox.batch.common;

/**
 * Represents a subtype of a batch job.
 */
public interface BatchJobSubType {

  /**
   * Returns the name of the batch job sub-type.
   * <p>
   * Used to achieve inheritance between enums of subtypes for different jobs.
   * @return Name of the batch job subtype as a string.
   */
  String name();
}
