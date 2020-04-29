package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "record_log")
public class RecordLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String recordId;

  private String datasetId;

  @Enumerated(EnumType.STRING)
  private Step step;

  @Enumerated(EnumType.STRING)
  private Status status;

  private String content;

  @OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
  private List<RecordErrorLogEntity> recordErrors = new ArrayList<>();

  public RecordLogEntity(String recordId, String datasetId, Step step,
      Status status, String content) {
    this.recordId = recordId;
    this.datasetId = datasetId;
    this.step = step;
    this.content = content;
    this.status = status;
  }

  public RecordLogEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public Step getStep() {
    return step;
  }

  public void setStep(Step step) {
    this.step = step;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status result) {
    this.status = result;
  }

  public List<RecordErrorLogEntity> getRecordErrors() {
    return recordErrors;
  }

  public void setRecordErrors(
      List<RecordErrorLogEntity> recordErrors) {
    this.recordErrors = recordErrors;
  }

  public void addRecordError(RecordErrorLogEntity recordErrorLogEntity) {
    recordErrors.add(recordErrorLogEntity);
    recordErrorLogEntity.setRecord(this);
  }

  public void removeRecordError(RecordErrorLogEntity recordErrorLogEntity) {
    recordErrors.remove(recordErrorLogEntity);
    recordErrorLogEntity.setRecord(null);
  }
}
