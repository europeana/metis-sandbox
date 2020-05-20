package eu.europeana.metis.sandbox.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dataset")
public class DatasetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer datasetId;

  private String datasetName;

  private Integer recordsQuantity;

  @Column(insertable = false, updatable = false)
  private LocalDateTime createdDate;

  public DatasetEntity(String datasetName, Integer recordsQuantity) {
    this.datasetName = datasetName;
    this.recordsQuantity = recordsQuantity;
  }

  public DatasetEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public String getDatasetId() {
    return String.valueOf(this.datasetId);
  }

  public String getDatasetName() {
    return this.datasetName;
  }

  public Integer getRecordsQuantity() {
    return this.recordsQuantity;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = Integer.valueOf(datasetId);
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public void setRecordsQuantity(Integer recordsQuantity) {
    this.recordsQuantity = recordsQuantity;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
