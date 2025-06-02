package eu.europeana.metis.sandbox.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity to map to thumbnail table
 */
@Entity
@Table(name = "thumbnail_id")
public class ThumbnailIdEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String datasetId;

  private String thumbnailId;

  public ThumbnailIdEntity(String datasetId, String thumbnailId) {
    this.datasetId = datasetId;
    this.thumbnailId = thumbnailId;
  }

  public ThumbnailIdEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getThumbnailId() {
    return thumbnailId;
  }

  public void setThumbnailId(String thumbnailId) {
    this.thumbnailId = thumbnailId;
  }
}
