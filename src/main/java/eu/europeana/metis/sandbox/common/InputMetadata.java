package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import lombok.Getter;

@Getter
public class InputMetadata {

  private String sourceExecutionId;
  private HarvestParametersEntity harvestParametersEntity;
  private TransformXsltEntity transformXsltEntity;

  public InputMetadata(String sourceExecutionId) {
    this.sourceExecutionId = sourceExecutionId;
  }

  public InputMetadata(String sourceExecutionId, InputMetadata inputMetadata) {
    this.sourceExecutionId = sourceExecutionId;
    this.harvestParametersEntity = inputMetadata.getHarvestParametersEntity();
    this.transformXsltEntity = inputMetadata.getTransformXsltEntity();
  }

  public InputMetadata(HarvestParametersEntity harvestParametersEntity, TransformXsltEntity transformXsltEntity) {
    this.harvestParametersEntity = harvestParametersEntity;
    this.transformXsltEntity = transformXsltEntity;
  }
}
