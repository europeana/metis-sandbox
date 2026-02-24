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
    @JsonSubTypes.Type(value = InternalInputMetadataRequest.class, name = "INTERNAL")
})
public sealed interface InputMetadataRequest
    permits OaiHarvestInputMetadataRequest, InternalInputMetadataRequest {
}
