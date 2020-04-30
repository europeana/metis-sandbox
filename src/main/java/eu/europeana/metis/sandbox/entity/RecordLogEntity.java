package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "record_log")
public class RecordLogEntity extends RecordEntity {

  private String content;

  public RecordLogEntity(String recordId, Integer datasetId,
      Step step, Status status, String content) {
    this.recordId = recordId;
    this.datasetId = datasetId;
    this.step = step;
    this.status = status;
    this.content = content;
  }

  public RecordLogEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
