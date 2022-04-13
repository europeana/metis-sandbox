package eu.europeana.metis.sandbox.entity.problempatterns;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "record_problem_pattern", indexes = {
    @Index(name = "record_problem_pattern_execution_point_id_record_id_pattern_key", columnList = "execution_point_id, record_id, pattern_id", unique = true)
})
public class RecordProblemPattern {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "record_problem_pattern_id", nullable = false)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "execution_point_id", nullable = false)
  private ExecutionPoint executionPoint;

  @Column(name = "record_id", nullable = false)
  private String recordId;

  @Column(name = "pattern_id", nullable = false, length = 10)
  private String patternId;

  public String getPatternId() {
    return patternId;
  }

  public void setPatternId(String patternId) {
    this.patternId = patternId;
  }

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public ExecutionPoint getExecutionPoint() {
    return executionPoint;
  }

  public void setExecutionPoint(ExecutionPoint executionPoint) {
    this.executionPoint = executionPoint;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
}
