package eu.europeana.metis.sandbox.common.task.input;

public record HttpHarvestInputMetadataRequest(
    String url,
    Integer stepSize
) implements HarvestInputMetadataRequest {

}
