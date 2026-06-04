package eu.europeana.metis.sandbox.common.task.input;

/**
 * Represents an input metadata request based on data of a previous execution.
 */
public sealed interface IntermediateInputMetadataRequest extends InputMetadataRequest
    permits SimpleIntermediateInputMetadataRequest, TransformExternalInputMetadataRequest {

  String sourceExecutionId();
}
