package eu.europeana.metis.sandbox.common.batch;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents the different types of full batch jobs by combining a {@link BatchJobType} with an optional {@link BatchJobSubType}.
 * This enumeration supports job-specific distinctions such as transform and validate.
 */
@Getter
public enum FullBatchJobType {
  HARVEST_OAI(BatchJobType.HARVEST_OAI, BatchJobGroup.HARVEST),
  HARVEST_FILE(BatchJobType.HARVEST_FILE, BatchJobGroup.HARVEST),
  TRANSFORM_EXTERNAL(BatchJobType.TRANSFORM, TransformationBatchJobSubType.EXTERNAL, BatchJobGroup.CURATE),
  VALIDATE_EXTERNAL(BatchJobType.VALIDATE, ValidationBatchJobSubType.EXTERNAL, BatchJobGroup.CURATE),
  TRANSFORM_INTERNAL(BatchJobType.TRANSFORM, TransformationBatchJobSubType.INTERNAL, BatchJobGroup.CURATE),
  VALIDATE_INTERNAL(BatchJobType.VALIDATE, ValidationBatchJobSubType.INTERNAL, BatchJobGroup.CURATE),
  NORMALIZE(BatchJobType.NORMALIZE, BatchJobGroup.CURATE),
  ENRICH(BatchJobType.ENRICH, BatchJobGroup.CURATE),
  MEDIA(BatchJobType.MEDIA, BatchJobGroup.CURATE),
  INDEX_PREVIEW(BatchJobType.INDEX, IndexBatchJobSubType.PREVIEW, BatchJobGroup.INDEX),
  DEBIAS(BatchJobType.DEBIAS, BatchJobGroup.DEBIAS);

  private final BatchJobType batchJobType;
  private final BatchJobSubType batchJobSubType;
  private final BatchJobGroup batchJobGroup;

  FullBatchJobType(BatchJobType batchJobType, BatchJobGroup batchJobGroup) {
    this.batchJobType = batchJobType;
    this.batchJobSubType = null;
    this.batchJobGroup = batchJobGroup;
  }

  FullBatchJobType(BatchJobType batchJobType, BatchJobSubType batchJobSubType, BatchJobGroup batchJobGroup) {
    this.batchJobType = batchJobType;
    this.batchJobSubType = batchJobSubType;
    this.batchJobGroup = batchJobGroup;
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

  /**
   * Represents a category of batch jobs.
   * <p>
   * Each constant in this enum corresponds to a broad category under which specific batch job types or subtypes may be
   * organized.
   */
  public enum BatchJobGroup {
    HARVEST, CURATE, INDEX, DEBIAS
  }
}
