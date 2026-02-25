package eu.europeana.metis.sandbox.common.task.input;

public record HttpHarvestInputMetadataRequest(
    String url
) implements InputMetadataRequest {

}
