package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class RecordLogEntity {

  @EmbeddedId
  private RecordLogEntityKey key;

  @Column(columnDefinition = "VARCHAR(MAX)")
  private String content;

  @Column
  @Enumerated(EnumType.STRING)
  private Status result;

  @Column
  private String error;

  public RecordLogEntity(RecordLogEntityKey key, String content, Status result) {
    this.key = key;
    this.content = content;
    this.result = result;
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
}
