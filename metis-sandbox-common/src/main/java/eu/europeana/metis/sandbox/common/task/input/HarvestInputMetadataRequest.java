package eu.europeana.metis.sandbox.common.task.input;

/**
 * Represents a specialized type of input metadata request associated with data harvesting.
 */
public sealed interface HarvestInputMetadataRequest extends InputMetadataRequest
    permits OaiHarvestInputMetadataRequest, HttpHarvestInputMetadataRequest {

  /**
   * Retrieves the step size used for skipping records (stepping over) during metadata harvesting.
   *
   * @return An {@code Integer} representing the step size, or {@code null} if not defined.
   */
  Integer stepSize();
}
