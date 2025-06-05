package eu.europeana.metis.sandbox.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to map to thumbnail table
 */
@Getter
@Setter
@NoArgsConstructor
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
}
