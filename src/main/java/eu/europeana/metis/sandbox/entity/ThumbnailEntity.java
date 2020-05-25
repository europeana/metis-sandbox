package eu.europeana.metis.sandbox.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity to map to thumbnail table
 */
@Entity
@Table(name = "thumbnail")
public class ThumbnailEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String datasetId;

  private String thumbnailId;

  public ThumbnailEntity(String datasetId, String thumbnailId) {
    this.datasetId = datasetId;
    this.thumbnailId = thumbnailId;
  }

  public ThumbnailEntity() {
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
