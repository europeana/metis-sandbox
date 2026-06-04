package eu.europeana.metis.sandbox.common.task.input;

public record TransformExternalInputMetadataRequest(String xslt, String sourceExecutionId) implements
    IntermediateInputMetadataRequest {

}
