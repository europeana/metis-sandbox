package eu.europeana.metis.sandbox.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity to map to record table
 */
@Entity
@Table(name = "record")
public class RecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected Long id;

  protected String europeanaId;

  @Column(name = "dataset_id")
  protected String datasetId;

  private String content;

  /**
   * Parameterized constructor
   *
   * @param europeanaId the europeana id associated to the record
   * @param datasetId the dataset id associated to the record
   * @param content the content of the record
   */
  public RecordEntity(String europeanaId, String datasetId, String content) {
    this.europeanaId = europeanaId;
    this.datasetId = datasetId;
    this.content = content;
  }


  public RecordEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEuropeanaId() {
    return europeanaId;
  }

  public void setEuropeanaId(String europeanaId) {
    this.europeanaId = europeanaId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
