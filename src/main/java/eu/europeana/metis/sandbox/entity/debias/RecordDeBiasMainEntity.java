package eu.europeana.metis.sandbox.entity.debias;

import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.service.debias.DeBiasSourceField;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Record debias entity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "record_debias_main")
public class RecordDeBiasMainEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(cascade = CascadeType.MERGE)
  @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
  private DatasetEntity datasetId;
  private String recordId;

  @Enumerated(EnumType.STRING)
  protected Language language;

  protected String literal;

  @Enumerated(EnumType.STRING)
  protected DeBiasSourceField sourceField;

  /**
   * Constructor.
   *
   * @param datasetId the associated dataset entity
   * @param recordId the unique identifier for the record
   * @param literal the literal value of the record
   * @param language the language of the record
   * @param sourceField the source field defining the record's origin
   */
  public RecordDeBiasMainEntity(DatasetEntity datasetId, String recordId, String literal, Language language,
      DeBiasSourceField sourceField) {
    this.datasetId = datasetId;
    this.recordId = recordId;
    this.literal = literal;
    this.language = language;
    this.sourceField = sourceField;
  }
}
