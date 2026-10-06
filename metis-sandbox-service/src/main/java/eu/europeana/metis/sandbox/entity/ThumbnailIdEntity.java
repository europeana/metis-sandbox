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
 * Thumbnail identifier entity.
 *
 * <p>Represents a mapping between a dataset ID and its associated thumbnail ID.
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

  /**
   * Constructor.
   *
   * @param datasetId the ID of the dataset associated with the thumbnail
   * @param thumbnailId the ID of the thumbnail associated with the dataset
   */
  public ThumbnailIdEntity(String datasetId, String thumbnailId) {
    this.datasetId = datasetId;
    this.thumbnailId = thumbnailId;
  }
}
