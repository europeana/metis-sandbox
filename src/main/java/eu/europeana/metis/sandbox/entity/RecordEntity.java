package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.domain.Record;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
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

  protected String providerId;

  protected String datasetId;

  protected String content;

  @OneToMany(mappedBy = "recordId")
  private List<RecordLogEntity> recordLogEntity;

  @OneToMany(mappedBy = "recordId")
  private List<RecordErrorLogEntity> recordErrorLogEntity;

  /**
   * Parameterized constructor
   *
   * @param europeanaId the europeana id associated to the record
   * @param datasetId the dataset id associated to the record
   * @param content the content of the record
   */
  public RecordEntity(String europeanaId, String providerId, String datasetId, String content) {
    this.europeanaId = europeanaId;
    this.providerId = providerId;
    this.datasetId = datasetId;
    this.content = content;
  }

  /**
   * Contructor
   *
   * @param record the record
   */
  public RecordEntity(Record record){
    this.europeanaId = record.getEuropeanaId();
    this.providerId = record.getProviderId();
    this.datasetId = record.getDatasetId();
    this.content = new String(record.getContent());
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

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
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

  public List<RecordLogEntity> getRecordLogEntity() {
    return recordLogEntity;
  }

  public void setRecordLogEntity(
      List<RecordLogEntity> recordLogEntity) {
    this.recordLogEntity = recordLogEntity;
  }

  public List<RecordErrorLogEntity> getRecordErrorLogEntity() {
    return recordErrorLogEntity;
  }

  public void setRecordErrorLogEntity(
      List<RecordErrorLogEntity> recordErrorLogEntity) {
    this.recordErrorLogEntity = recordErrorLogEntity;
  }
}
