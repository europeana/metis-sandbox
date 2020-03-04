package eu.europeana.metis.sandbox.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class DatasetEntity {

  @Id
  @Column
  private String datasetId;

  @Column
  private String datasetName;

  @Column
  private Integer recordsQuantity;

  public DatasetEntity(String datasetId, String datasetName, Integer recordsQuantity) {
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.recordsQuantity = recordsQuantity;
  }
}
