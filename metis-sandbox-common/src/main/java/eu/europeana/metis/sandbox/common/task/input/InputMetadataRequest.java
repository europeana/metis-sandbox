package eu.europeana.metis.sandbox.common.task.input;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Represents an input data endpoint used in the processing engine.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OaiHarvestInputMetadataRequest.class, name = "OAI"),
    @JsonSubTypes.Type(value = HttpHarvestInputMetadataRequest.class, name = "HTTP"),
    @JsonSubTypes.Type(value = SimpleIntermediateInputMetadataRequest.class, name = "SIMPLE_INTERMEDIATE"),
    @JsonSubTypes.Type(value = TransformExternalInputMetadataRequest.class, name = "TRANSFORM_EXTERNAL"),
    @JsonSubTypes.Type(value = TransformInternalInputMetadataRequest.class, name = "TRANSFORM_INTERNAL")
})
public sealed interface InputMetadataRequest permits HarvestInputMetadataRequest, IntermediateInputMetadataRequest {

}
