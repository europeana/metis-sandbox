package eu.europeana.metis.sandbox.entity.problempatterns;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Entity class for record problem pattern.
 */
@Entity
@Table(schema = "problem_patterns", name = "record_problem_pattern", indexes = {
    @Index(name = "record_problem_pattern_execution_point_id_record_id_pattern_key", columnList = "execution_point_id, record_id, pattern_id", unique = true)
})
public class RecordProblemPattern {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "record_problem_pattern_id", nullable = false)
  private Integer recordProblemPatternId;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "execution_point_id", nullable = false)
  private ExecutionPoint executionPoint;

  @Column(name = "record_id", nullable = false)
  private String recordId;

  @Column(name = "pattern_id", nullable = false, length = 10)
  private String patternId;

  @OneToMany(mappedBy = "recordProblemPattern", fetch = FetchType.EAGER)
  private Set<RecordProblemPatternOccurrence> recordProblemPatternOccurrences = new LinkedHashSet<>();

  public Set<RecordProblemPatternOccurrence> getRecordProblemPatternOccurences() {
    return new LinkedHashSet<>(recordProblemPatternOccurrences);
  }

  public void setRecordProblemPatternOccurrences(
      Set<RecordProblemPatternOccurrence> recordProblemPatternOccurrences) {
    this.recordProblemPatternOccurrences =
        recordProblemPatternOccurrences == null ? new LinkedHashSet<>() : new LinkedHashSet<>(recordProblemPatternOccurrences);
  }

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

  public Integer getRecordProblemPatternId() {
    return recordProblemPatternId;
  }

  public void setRecordProblemPatternId(Integer id) {
    this.recordProblemPatternId = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordProblemPattern that = (RecordProblemPattern) o;
    return Objects.equals(recordProblemPatternId, that.recordProblemPatternId) && Objects.equals(executionPoint,
        that.executionPoint) && Objects.equals(recordId, that.recordId) && Objects.equals(patternId,
        that.patternId) && Objects.equals(recordProblemPatternOccurrences, that.recordProblemPatternOccurrences);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordProblemPatternId, executionPoint, recordId, patternId, recordProblemPatternOccurrences);
  }
}
