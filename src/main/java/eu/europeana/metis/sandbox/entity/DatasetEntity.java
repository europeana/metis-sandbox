package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dataset entity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dataset")
public class DatasetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer datasetId;

  @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT NOW()", insertable = false, updatable = false)
  private ZonedDateTime createdDate;

  private String datasetName;

  @Enumerated(EnumType.STRING)
  private WorkflowType workflowType;

  @Enumerated(EnumType.STRING)
  private Language language;

  @Enumerated(EnumType.STRING)
  private Country country;

  private String createdById;

  /**
   * Constructor.
   *
   * @param datasetName the name of the dataset
   * @param workflowType the workflow type associated with the dataset
   * @param language the language of the dataset
   * @param country the country of the dataset
   * @param createdById the ID of the user who created the dataset
   */
  public DatasetEntity(String datasetName, WorkflowType workflowType, Language language, Country country, String createdById) {
    this.workflowType = workflowType;
    this.datasetName = datasetName;
    this.createdById = createdById;
    this.language = language;
    this.country = country;
  }
}
