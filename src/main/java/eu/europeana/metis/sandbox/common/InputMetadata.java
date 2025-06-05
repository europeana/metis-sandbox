package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InputMetadata {

  private String sourceExecutionId;
  private HarvestParametersEntity harvestParametersEntity;
  private TransformXsltEntity transformXsltEntity;
  private Integer stepSize;

  public InputMetadata(String sourceExecutionId) {
    this.sourceExecutionId = sourceExecutionId;
  }

  public InputMetadata(String sourceExecutionId, InputMetadata inputMetadata) {
    this.sourceExecutionId = sourceExecutionId;
    this.harvestParametersEntity = inputMetadata.getHarvestParametersEntity();
    this.transformXsltEntity = inputMetadata.getTransformXsltEntity();
  }

  public InputMetadata(HarvestParametersEntity harvestParametersEntity, TransformXsltEntity transformXsltEntity, Integer stepSize) {
    this.harvestParametersEntity = harvestParametersEntity;
    this.transformXsltEntity = transformXsltEntity;
    this.stepSize = stepSize;
  }
}
