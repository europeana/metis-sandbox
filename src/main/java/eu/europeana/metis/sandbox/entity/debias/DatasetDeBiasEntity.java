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

  /**
   * Gets id.
   *
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets dataset id.
   *
   * @return the dataset id
   */
  public DatasetEntity getDatasetId() {
    return datasetId;
  }

  /**
   * Sets dataset id.
   *
   * @param datasetId the dataset id
   */
  public void setDatasetId(DatasetEntity datasetId) {
    this.datasetId = datasetId;
  }

  /**
   * Gets state.
   *
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Sets state.
   *
   * @param state the state
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * Gets created date.
   *
   * @return the created date
   */
  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  /**
   * Sets created date.
   *
   * @param createdDate the created date
   */
  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
