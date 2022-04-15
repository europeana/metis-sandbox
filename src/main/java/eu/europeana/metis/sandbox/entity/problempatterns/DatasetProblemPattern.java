package eu.europeana.metis.sandbox.entity.problempatterns;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

/**
 * Entity class for dataset problem pattern.
 */
@Entity
@Table(schema = "problem_patterns", name = "dataset_problem_pattern")
public class DatasetProblemPattern {

  @EmbeddedId
  private DatasetProblemPatternId datasetProblemPatternId;
  @MapsId("executionPointId")
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "execution_point_id")
  private ExecutionPoint executionPoint;

  @Column(name = "record_occurences", nullable = false)
  private Integer recordOccurences;

  public DatasetProblemPattern() {
    //Required for JPA
  }

  /**
   * Constructor with required parameters.
   *
   * @param datasetProblemPatternId the dataset problem pattern id
   * @param executionPoint the execution point
   * @param recordOccurences the record occurences
   */
  public DatasetProblemPattern(DatasetProblemPatternId datasetProblemPatternId,
      ExecutionPoint executionPoint, Integer recordOccurences) {
    this.datasetProblemPatternId = datasetProblemPatternId;
    this.executionPoint = executionPoint;
    this.recordOccurences = recordOccurences;
  }

  public ExecutionPoint getExecutionPoint() {
    return executionPoint;
  }

  public void setExecutionPoint(ExecutionPoint executionPoint) {
    this.executionPoint = executionPoint;
  }

  public DatasetProblemPatternId getDatasetProblemPatternId() {
    return datasetProblemPatternId;
  }

  public void setDatasetProblemPatternId(DatasetProblemPatternId id) {
    this.datasetProblemPatternId = id;
  }

  public Integer getRecordOccurences() {
    return recordOccurences;
  }

  public void setRecordOccurences(Integer recordOccurences) {
    this.recordOccurences = recordOccurences;
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
    return Objects.equals(datasetProblemPatternId, that.datasetProblemPatternId) && Objects.equals(
        executionPoint, that.executionPoint) && Objects.equals(recordOccurences, that.recordOccurences);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetProblemPatternId, executionPoint, recordOccurences);
  }
}
