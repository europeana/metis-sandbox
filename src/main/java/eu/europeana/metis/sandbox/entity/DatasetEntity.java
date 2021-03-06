package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity to map dataset table
 */
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

  @Enumerated(EnumType.STRING)
  private Language language;

  @Enumerated(EnumType.STRING)
  private Country country;

  public DatasetEntity(String datasetName, Integer recordsQuantity, Language language, Country country) {
    this.datasetName = datasetName;
    this.recordsQuantity = recordsQuantity;
    this.language = language;
    this.country = country;
  }

  public DatasetEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public Integer getRecordsQuantity() {
    return recordsQuantity;
  }

  public void setRecordsQuantity(Integer recordsQuantity) {
    this.recordsQuantity = recordsQuantity;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

}
