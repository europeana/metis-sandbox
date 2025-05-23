package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entity to map to record_log table
 */
@Entity
@Table(name = "record_log")
public class RecordLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "record_id", referencedColumnName = "id")
  private RecordEntity recordId;

  @Column(columnDefinition="TEXT")
  protected String content;

  @Enumerated(EnumType.STRING)
  protected Step step;

  @Enumerated(EnumType.STRING)
  protected Status status;

  /**
   * Parameterized constructor
   * @param recordId the record id
   * @param step the workflow step
   * @param status the status of the record
   */

  public RecordLogEntity(RecordEntity recordId, String content, Step step, Status status) {
    this.recordId = recordId;
    this.content = content;
    this.step = step;
    this.status = status;

  }

  public RecordLogEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public RecordEntity getRecordId() {
    return recordId;
  }

  public void setRecordId(RecordEntity recordId) {
    this.recordId = recordId;
  }

  public Step getStep() {
    return step;
  }

  public void setStep(Step step) {
    this.step = step;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
