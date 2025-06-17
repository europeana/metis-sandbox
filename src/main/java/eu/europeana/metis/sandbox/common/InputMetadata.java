package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import lombok.Getter;

/**
 * Represents metadata associated with input parameters for execution.
 *
 * <p>Provides constructors to initialize the metadata with specific combinations of these fields.
 */
@Getter
public class InputMetadata {

  private String sourceExecutionId;
  private HarvestParametersEntity harvestParametersEntity;
  private TransformXsltEntity transformXsltEntity;

  /**
   * Constructor with a source execution identifier.
   *
   * @param sourceExecutionId The unique identifier of the source execution.
   */
  public InputMetadata(String sourceExecutionId) {
    this.sourceExecutionId = sourceExecutionId;
  }

  /**
   * Constructor with a source execution ID and another InputMetadata instance to copy harvest parameters and transform XSLT entity.
   *
   * @param sourceExecutionId The unique identifier of the source execution.
   * @param inputMetadata The existing InputMetadata instance from which to copy harvest parameters and transform XSLT entity.
   */
  public InputMetadata(String sourceExecutionId, InputMetadata inputMetadata) {
    this.sourceExecutionId = sourceExecutionId;
    this.harvestParametersEntity = inputMetadata.getHarvestParametersEntity();
    this.transformXsltEntity = inputMetadata.getTransformXsltEntity();
  }

  /**
   * Constructor with harvest parameters and transform XSLT entity.
   *
   * @param harvestParametersEntity The harvest parameters entity containing details about the dataset and step size.
   * @param transformXsltEntity The transformation XSLT entity with information about dataset-specific XSLT configuration.
   */
  public InputMetadata(HarvestParametersEntity harvestParametersEntity, TransformXsltEntity transformXsltEntity) {
    this.harvestParametersEntity = harvestParametersEntity;
    this.transformXsltEntity = transformXsltEntity;
  }
}
