package eu.europeana.metis.sandbox.batch.common;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.tika.utils.StringUtils;

public enum FullBatchJobType {
  OAI_HARVEST(BatchJobType.OAI_HARVEST),
  FILE_HARVEST(BatchJobType.FILE_HARVEST),
  VALIDATION_EXTERNAL(BatchJobType.VALIDATION, ValidationBatchJobSubType.EXTERNAL),
  VALIDATION_INTERNAL(BatchJobType.VALIDATION, ValidationBatchJobSubType.INTERNAL),
  TRANSFORMATION_EXTERNAL(BatchJobType.TRANSFORMATION, TransformationBatchJobSubType.EXTERNAL),
  TRANSFORMATION_INTERNAL(BatchJobType.TRANSFORMATION, TransformationBatchJobSubType.INTERNAL),
  NORMALIZATION(BatchJobType.NORMALIZATION),
  ENRICHMENT(BatchJobType.ENRICHMENT),
  MEDIA(BatchJobType.MEDIA),
  INDEX(BatchJobType.INDEX),
  DEBIAS(BatchJobType.DEBIAS);

  private final BatchJobType batchJobType;
  private final BatchJobSubType batchJobSubType;

  FullBatchJobType(BatchJobType batchJobType) {
    this.batchJobType = batchJobType;
    this.batchJobSubType = null;
  }

  FullBatchJobType(BatchJobType batchJobType, BatchJobSubType batchJobSubType) {
    this.batchJobType = batchJobType;
    this.batchJobSubType = batchJobSubType;
  }

  public BatchJobType getBatchJobType() {
    return batchJobType;
  }

  public BatchJobSubType getBatchJobSubType() {
    return batchJobSubType;
  }

  public static FullBatchJobType validateAndGetFullBatchJobType(BatchJobType batchJobType, BatchJobSubType batchJobSubType) {
    return validateAndGetFullBatchJobType(batchJobType.name(),
        Optional.ofNullable(batchJobSubType).map(BatchJobSubType::name).orElse(null));
  }

  public static FullBatchJobType validateAndGetFullBatchJobType(String prefix, String suffix) {
    BatchJobType matchedBatchJobType = Arrays.stream(BatchJobType.values()).filter(f -> f.name().equals(prefix)).findFirst()
                                             .orElseThrow();
    if (StringUtils.isBlank(suffix)) {
      return FullBatchJobType.valueOf(matchedBatchJobType.name());
    }

    List<FullBatchJobType> candidateFullBatchJobTypes = Arrays.stream(FullBatchJobType.values())
                                                              .filter(f -> f.getBatchJobType().equals(matchedBatchJobType))
                                                              .toList();

    return candidateFullBatchJobTypes.stream().filter(f -> f.getBatchJobSubType().name().equals(suffix)).findFirst()
                                     .orElseThrow();
  }
}
