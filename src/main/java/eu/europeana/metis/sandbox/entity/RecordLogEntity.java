package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Status;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
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
}
