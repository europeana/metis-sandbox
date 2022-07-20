package eu.europeana.metis.sandbox.entity.problempatterns;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

/**
 * Entity class for record titles
 */
@Entity
@Table(name = "record_title", schema = "problem_patterns")
public class RecordTitle {

  @EmbeddedId
  private RecordTitleCompositeKey recordTitleCompositeKey;

  @MapsId("executionPointId")
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "execution_point_id", nullable = false)
  private ExecutionPoint executionPoint;

  public RecordTitle() {
    //Required for JPA
  }

  /**
   * Constructor with required parameters.
   *
   * @param recordTitleCompositeKey the record title composity key
   * @param executionPoint the execution point
   */
  public RecordTitle(RecordTitleCompositeKey recordTitleCompositeKey,
      ExecutionPoint executionPoint) {
    this.recordTitleCompositeKey = recordTitleCompositeKey;
    this.executionPoint = executionPoint;
  }

  public ExecutionPoint getExecutionPoint() {
    return executionPoint;
  }

  public void setExecutionPoint(ExecutionPoint executionPoint) {
    this.executionPoint = executionPoint;
  }

  public RecordTitleCompositeKey getRecordTitleCompositeKey() {
    return recordTitleCompositeKey;
  }

  public void setRecordTitleCompositeKey(RecordTitleCompositeKey id) {
    this.recordTitleCompositeKey = id;
  }
}
