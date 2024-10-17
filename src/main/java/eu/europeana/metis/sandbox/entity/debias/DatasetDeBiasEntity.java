package eu.europeana.metis.sandbox.entity.debias;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

/**
 * The type Detection entity.
 */
@Entity
@Table(name = "dataset_debias_detect")
public class DatasetDeBiasEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(cascade = CascadeType.MERGE)
  @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
  private DatasetEntity datasetId;

  private String state;

  @Column(insertable = false, updatable = false)
  private ZonedDateTime createdDate;


  /**
   * Instantiates a new Detection entity.
   */
  public DatasetDeBiasEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  /**
   * Instantiates a new Detection entity.
   *
   * @param datasetId the dataset id
   * @param state the state
   */
  public DatasetDeBiasEntity(DatasetEntity datasetId, String state) {
    this.datasetId = datasetId;
    this.state = state;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DatasetEntity getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(DatasetEntity datasetId) {
    this.datasetId = datasetId;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
