package eu.europeana.metis.sandbox.common.task.input;

/**
 * Represents an input metadata request based on data of a previous execution.
 *
 * @param sourceExecutionId The identifier of the previous execution.
 */
public record IntermediateInputMetadataRequest(String sourceExecutionId) implements InputMetadataRequest {

}
