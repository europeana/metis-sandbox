package eu.europeana.metis.sandbox.common.task.input;

public sealed interface HarvestInputMetadataRequest extends InputMetadataRequest
    permits OaiHarvestInputMetadataRequest, HttpHarvestInputMetadataRequest {

  Integer stepSize();
}
