package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "record_log")
public class RecordLogEntity {

  @Id
  @EmbeddedId
  private RecordLogEntityKey key;

  @Column
  private String content;

  @Column
  @Enumerated(EnumType.STRING)
  private Status result;

  @Column
  private String error;

  @Column
  private String stackTrace;

  public RecordLogEntity(RecordLogEntityKey key, String content, Status result, String error, String stackTrace) {
    this.key = key;
    this.content = content;
    this.result = result;
    this.error = error;
    this.stackTrace = stackTrace;
  }

  public RecordLogEntity() {
  }

  public RecordLogEntityKey getKey() {
    return this.key;
  }

  public String getContent() {
    return this.content;
  }

  public Status getResult() {
    return this.result;
  }

  public String getError() {
    return this.error;
  }

  public void setKey(RecordLogEntityKey key) {
    this.key = key;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setResult(Status result) {
    this.result = result;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }
}
