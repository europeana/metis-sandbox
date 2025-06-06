package eu.europeana.metis.sandbox.batch.common;

import java.util.Arrays;
import java.util.List;
import org.apache.tika.utils.StringUtils;

public enum FullBatchJobType {
  HARVEST_OAI(BatchJobType.HARVEST_OAI),
  HARVEST_FILE(BatchJobType.HARVEST_FILE),
  TRANSFORM_EXTERNAL(BatchJobType.TRANSFORM, TransformationBatchJobSubType.EXTERNAL),
  VALIDATE_EXTERNAL(BatchJobType.VALIDATE, ValidationBatchJobSubType.EXTERNAL),
  TRANSFORM_INTERNAL(BatchJobType.TRANSFORM, TransformationBatchJobSubType.INTERNAL),
  VALIDATE_INTERNAL(BatchJobType.VALIDATE, ValidationBatchJobSubType.INTERNAL),
  NORMALIZE(BatchJobType.NORMALIZE),
  ENRICH(BatchJobType.ENRICH),
  MEDIA(BatchJobType.MEDIA),
  INDEX_PUBLISH(BatchJobType.INDEX, IndexBatchJobSubType.PUBLISH),
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
