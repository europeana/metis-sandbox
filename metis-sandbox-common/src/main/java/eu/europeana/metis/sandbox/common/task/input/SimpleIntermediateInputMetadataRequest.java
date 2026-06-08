package eu.europeana.metis.sandbox.common.task.input;

/**
 * Represents a simple input metadata request that refers to the metadata of a previous execution using a source execution
 * identifier.
 *
 * @param sourceExecutionId A {@code String} representing the source execution ID used to retrieve the metadata for the current
 * request.
 */
public record SimpleIntermediateInputMetadataRequest(String sourceExecutionId) implements IntermediateInputMetadataRequest {

}
