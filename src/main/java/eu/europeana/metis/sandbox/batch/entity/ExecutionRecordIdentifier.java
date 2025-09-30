package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ExecutionRecordIdentifier {

  @Column(length = 300)
  private String externalRecordId;

  @Column(length = 300)
  private String sourceRecordId;

  @Column(length = 300)
  private String recordId;
}
