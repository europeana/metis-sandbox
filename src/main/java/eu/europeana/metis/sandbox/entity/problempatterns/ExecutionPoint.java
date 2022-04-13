package eu.europeana.metis.sandbox.entity.problempatterns;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "execution_point", indexes = {
    @Index(name = "execution_point_dataset_id_execution_step_execution_timesta_key",
        columnList = "dataset_id, execution_step, execution_timestamp", unique = true)
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
  private OffsetDateTime executionTimestamp;

  @OneToMany(mappedBy = "executionPoint")
  private Set<DatasetProblemPattern> datasetProblemPatterns = new LinkedHashSet<>();

  public Set<DatasetProblemPattern> getDatasetProblemPatterns() {
    return datasetProblemPatterns;
  }

  public void setDatasetProblemPatterns(Set<DatasetProblemPattern> datasetProblemPatterns) {
    this.datasetProblemPatterns = datasetProblemPatterns;
  }

  public OffsetDateTime getExecutionTimestamp() {
    return executionTimestamp;
  }

  public void setExecutionTimestamp(OffsetDateTime executionTimestamp) {
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
