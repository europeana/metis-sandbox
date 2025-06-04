package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.utils.CompressedFileExtension;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InputMetadata {

  private HarvestParametersEntity harvestParametersEntity;
  private TransformXsltEntity transformXsltEntity;
  private String sourceExecutionId;
  private String url;
  private String setSpec;
  private String metadataFormat;
  private Integer stepSize;
  private CompressedFileExtension compressedFileExtension;

  public InputMetadata(String sourceExecutionId) {
    this.sourceExecutionId = sourceExecutionId;
  }

  public InputMetadata(String sourceExecutionId, InputMetadata inputMetadata) {
    this.sourceExecutionId = sourceExecutionId;
    this.harvestParametersEntity = inputMetadata.getHarvestParametersEntity();
    this.transformXsltEntity = inputMetadata.getTransformXsltEntity();
    this.url = inputMetadata.getUrl();
    this.setSpec = inputMetadata.getSetSpec();
    this.metadataFormat = inputMetadata.getMetadataFormat();
    this.stepSize = inputMetadata.getStepSize();
    this.compressedFileExtension = inputMetadata.getCompressedFileExtension();
  }

  public InputMetadata(String url, String setSpec, String metadataFormat, Integer stepSize, TransformXsltEntity transformXsltEntity) {
    this.url = url;
    this.setSpec = setSpec;
    this.metadataFormat = metadataFormat;
    this.stepSize = stepSize;
    this.transformXsltEntity = transformXsltEntity;
  }

  public InputMetadata(HarvestParametersEntity harvestParametersEntity, CompressedFileExtension compressedFileExtension, Integer stepSize, TransformXsltEntity transformXsltEntity) {
    this.stepSize = stepSize;
    this.harvestParametersEntity = harvestParametersEntity;
    this.compressedFileExtension = compressedFileExtension;
    this.transformXsltEntity = transformXsltEntity;
  }

  public InputMetadata(HarvestParametersEntity harvestParametersEntity) {
    this.harvestParametersEntity = harvestParametersEntity;
  }
}
