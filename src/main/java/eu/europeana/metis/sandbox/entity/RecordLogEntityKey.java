package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Step;
import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class RecordLogEntityKey implements Serializable {

  private String id;

  private String datasetId;

  @Enumerated(EnumType.STRING)
  private Step step;

  public RecordLogEntityKey(String id, String datasetId, Step step) {
    this.id = id;
    this.datasetId = datasetId;
    this.step = step;
  }
}
