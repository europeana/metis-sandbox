package eu.europeana.metis.sandbox.entity.problempatterns;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Entity class for dataset problem pattern.
 */
@Entity
@Table(schema = "problem_patterns", name = "dataset_problem_pattern", indexes = {
    @Index(name = "dataset_problem_pattern_pattern_id_idx", columnList = "pattern_id")
})
public class DatasetProblemPattern {

  @EmbeddedId
  private DatasetProblemPatternCompositeKey datasetProblemPatternCompositeKey;
  @MapsId("executionPointId")
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "execution_point_id")
  private ExecutionPoint executionPoint;

  @Column(name = "record_occurrences", nullable = false)
  private Integer recordOccurrences;

  public DatasetProblemPattern() {
    //Required for JPA
  }

  /**
   * Constructor with required parameters.
   *
   * @param datasetProblemPatternCompositeKey the dataset problem pattern id
   * @param executionPoint the execution point
   * @param recordOccurrences the record occurrences
   */
  public DatasetProblemPattern(DatasetProblemPatternCompositeKey datasetProblemPatternCompositeKey,
      ExecutionPoint executionPoint, Integer recordOccurrences) {
    this.datasetProblemPatternCompositeKey = datasetProblemPatternCompositeKey;
    this.executionPoint = executionPoint;
    this.recordOccurrences = recordOccurrences;
  }

  public ExecutionPoint getExecutionPoint() {
    return executionPoint;
  }

  public void setExecutionPoint(ExecutionPoint executionPoint) {
    this.executionPoint = executionPoint;
  }

  public DatasetProblemPatternCompositeKey getDatasetProblemPatternCompositeKey() {
    return datasetProblemPatternCompositeKey;
  }

  public void setDatasetProblemPatternCompositeKey(DatasetProblemPatternCompositeKey datasetProblemPatternCompositeKey) {
    this.datasetProblemPatternCompositeKey = datasetProblemPatternCompositeKey;
  }

  public Integer getRecordOccurrences() {
    return recordOccurrences;
  }

  public void setRecordOccurrences(Integer recordOccurences) {
    this.recordOccurrences = recordOccurences;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DatasetProblemPattern that = (DatasetProblemPattern) o;
    return Objects.equals(datasetProblemPatternCompositeKey, that.datasetProblemPatternCompositeKey) && Objects.equals(
        executionPoint, that.executionPoint) && Objects.equals(recordOccurrences, that.recordOccurrences);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetProblemPatternCompositeKey, executionPoint, recordOccurrences);
  }
}
