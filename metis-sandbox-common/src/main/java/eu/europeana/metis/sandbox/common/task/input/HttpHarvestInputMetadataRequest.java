package eu.europeana.metis.sandbox.common.task.input;

/**
 * Represents an input data endpoint for HTTP-based metadata harvesting.
 *
 * @param url The URL of the HTTP harvest file.
 * @param stepSize The step size used for skipping records during the metadata harvesting process.
 *                 It may be {@code null} if no specific step size is defined.
 */
public record HttpHarvestInputMetadataRequest(
    String url,
    Integer stepSize
) implements HarvestInputMetadataRequest {

}
