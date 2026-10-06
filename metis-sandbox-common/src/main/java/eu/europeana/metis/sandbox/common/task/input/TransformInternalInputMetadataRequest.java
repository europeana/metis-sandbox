package eu.europeana.metis.sandbox.common.task.input;

/**
 * Represents a request for transforming external metadata into a new format using an XSLT transformation and a reference to a
 * previous execution id.
 * <p>
 *
 * @param xslt A {@code String} representing the XSLT transformation that will be applied to the source metadata.
 * @param sourceExecutionId A {@code String} representing the identifier of the execution whose output metadata serves as input
 * for the current transformation request.
 */
public record TransformInternalInputMetadataRequest(String xslt, String sourceExecutionId) implements
    IntermediateInputMetadataRequest {

}
