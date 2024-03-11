package eu.europeana.metis.sandbox.entity.problempatterns;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity class for execution point.
 */
@Entity
@Table(schema = "problem_patterns", name = "execution_point", indexes = {
    @Index(name = "execution_point_dataset_id_execution_step_execution_timesta_key",
        columnList = "dataset_id, execution_step, execution_timestamp", unique = true),
    @Index(name = "execution_point_dataset_id_execution_step_idx", columnList = "dataset_id, execution_step")
})
public class ExecutionPoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "execution_point_id", nullable = false)
  private Integer executionPointId;

  @Column(name = "dataset_id", nullable = false, length = 20)
  private String datasetId;

  @Column(name = "execution_step", nullable = false, length = 20)
  private String executionStep;

  @Column(name = "execution_timestamp", nullable = false)
  private LocalDateTime executionTimestamp;

  @OneToMany(mappedBy = "executionPoint", fetch = FetchType.EAGER)
  private Set<DatasetProblemPattern> datasetProblemPatterns = new LinkedHashSet<>();

  @OneToMany(mappedBy = "executionPoint", fetch = FetchType.EAGER)
  private Set<RecordProblemPattern> recordProblemPatterns = new LinkedHashSet<>();

  public ExecutionPoint() {
    //Required for JPA
  }

  public Set<RecordProblemPattern> getRecordProblemPatterns() {
    return new LinkedHashSet<>(recordProblemPatterns);
  }

  public void setRecordProblemPatterns(
      Set<RecordProblemPattern> recordProblemPatterns) {
    this.recordProblemPatterns =
        recordProblemPatterns == null ? new LinkedHashSet<>() : new LinkedHashSet<>(recordProblemPatterns);
  }

  public Set<DatasetProblemPattern> getDatasetProblemPatterns() {
    return new LinkedHashSet<>(datasetProblemPatterns);
  }

  public void setDatasetProblemPatterns(Set<DatasetProblemPattern> datasetProblemPatterns) {
    this.datasetProblemPatterns =
        datasetProblemPatterns == null ? new LinkedHashSet<>() : new LinkedHashSet<>(datasetProblemPatterns);
  }

  public LocalDateTime getExecutionTimestamp() {
    return executionTimestamp;
  }

  public void setExecutionTimestamp(LocalDateTime executionTimestamp) {
    this.executionTimestamp = executionTimestamp;
  }

  public String getExecutionStep() {
    return executionStep;
  }

  public void setExecutionStep(String executionStep) {
    this.executionStep = executionStep;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public Integer getExecutionPointId() {
    return executionPointId;
  }

  public void setExecutionPointId(Integer id) {
    this.executionPointId = id;
  }
}
