package eu.europeana.metis.sandbox.common.task.input;

/**
 * Represents an input metadata request based on data of a previous execution.
 */
public sealed interface IntermediateInputMetadataRequest extends InputMetadataRequest
    permits SimpleIntermediateInputMetadataRequest, TransformExternalInputMetadataRequest, TransformInternalInputMetadataRequest {

  /**
   * Retrieves the identifier of the source execution used as input for the current metadata request.
   *
   * @return A {@code String} representing the source execution ID.
   */
  String sourceExecutionId();
}
