package eu.europeana.metis.sandbox.domain;

import eu.europeana.metis.utils.CompressedFileExtension;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InputMetadata {
  private String sourceExecutionId;
  private String url;
  private String setSpec;
  private String metadataFormat;
  private Integer stepSize;
  private Path datasetRecordsCompressedFilePath;
  private CompressedFileExtension compressedFileExtension;

  public InputMetadata(String sourceExecutionId) {
    this.sourceExecutionId = sourceExecutionId;
  }

  public InputMetadata(String url, String setSpec, String metadataFormat, Integer stepSize) {
    this.url = url;
    this.setSpec = setSpec;
    this.metadataFormat = metadataFormat;
    this.stepSize = stepSize;
  }

  public InputMetadata(Path datasetRecordsCompressedFilePath, CompressedFileExtension compressedFileExtension, Integer stepSize) {
    this.stepSize = stepSize;
    this.datasetRecordsCompressedFilePath = datasetRecordsCompressedFilePath;
    this.compressedFileExtension = compressedFileExtension;
  }

  public InputMetadata(Path datasetRecordsCompressedFilePath) {
    this.datasetRecordsCompressedFilePath = datasetRecordsCompressedFilePath;
  }
}
