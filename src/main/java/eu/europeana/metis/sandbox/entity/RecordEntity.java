package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "record")
public class RecordEntity {

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

  @OneToMany(mappedBy = "record")
  private List<RecordErrorEntity> recordErrors;

  public RecordEntity(String recordId, String datasetId, Step step,
      Status status, String content) {
    this.recordId = recordId;
    this.datasetId = datasetId;
    this.step = step;
    this.content = content;
    this.status = status;
  }

  public RecordEntity() {
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

  public List<RecordErrorEntity> getRecordErrors() {
    return recordErrors;
  }

  public void setRecordErrors(
      List<RecordErrorEntity> recordErrors) {
    this.recordErrors = recordErrors;
  }
}
