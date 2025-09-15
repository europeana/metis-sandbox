package eu.europeana.metis.sandbox.batch.common;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.tika.utils.StringUtils;

/**
 * Represents the different types of full batch jobs by combining a {@link BatchJobType} with an optional {@link BatchJobSubType}.
 * This enumeration supports job-specific distinctions such as transform and validate.
 */
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

  /**
   * Validates and returns the corresponding FullBatchJobType based on the given prefix and suffix.
   *
   * @param prefix The name of the BatchJobType to validate and match.
   * @param suffix The specific BatchJobSubType name, used to refine the match. Can be blank or null.
   * @return The matched FullBatchJobType corresponding to the prefix and suffix.
   * @throws NoSuchElementException If no match is found for the given prefix or suffix.
   */
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
