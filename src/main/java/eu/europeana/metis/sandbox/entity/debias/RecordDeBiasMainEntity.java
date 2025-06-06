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

/**
 * Entity to map to record_debias_main table
 */
@Entity
@Table(name = "record_debias_main")
public class RecordDeBiasMainEntity {

  /**
   * Primary key
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  /**
   * Reference to record od
   */
  @ManyToOne(cascade = CascadeType.MERGE)
  @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
  private DatasetEntity datasetId;
  private String recordId;
  /**
   * The Language.
   */
  @Enumerated(EnumType.STRING)
  protected Language language;
  /**
   * The Literal.
   */
  protected String literal;
  /**
   * The DeBias source field.
   */
  @Enumerated(EnumType.STRING)
  protected DeBiasSourceField sourceField;

  /**
   * Parameterized constructor
   *
   * @param recordId the record id
   * @param literal the literal
   * @param language the language
   * @param sourceField the DeBias source field
   */
  public RecordDeBiasMainEntity(DatasetEntity datasetId, String recordId, String literal, Language language,
      DeBiasSourceField sourceField) {
    this.datasetId = datasetId;
    this.recordId = recordId;
    this.literal = literal;
    this.language = language;
    this.sourceField = sourceField;
  }

  /**
   * Instantiates a new Record de bias main entity.
   */
  public RecordDeBiasMainEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  /**
   * Gets record id.
   *
   * @return the record id
   */
  public String getRecordId() {
    return recordId;
  }

  /**
   * Sets record id.
   *
   * @param recordId the record id
   */
  public void setRecordId(String recordId) {
    this.recordId = recordId;
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
   * Gets language.
   *
   * @return the language
   */
  public Language getLanguage() {
    return language;
  }

  /**
   * Sets language.
   *
   * @param language the language
   */
  public void setLanguage(Language language) {
    this.language = language;
  }

  /**
   * Gets literal.
   *
   * @return the literal
   */
  public String getLiteral() {
    return literal;
  }

  /**
   * Sets literal.
   *
   * @param literal the literal
   */
  public void setLiteral(String literal) {
    this.literal = literal;
  }

  /**
   * Gets DeBias source field.
   *
   * @return the DeBias source field
   */
  public DeBiasSourceField getSourceField() {
    return sourceField;
  }

  /**
   * Sets DeBias source field.
   *
   * @param deBiasSourceField the DeBias source field
   */
  public void setSourceField(DeBiasSourceField deBiasSourceField) {
    this.sourceField = deBiasSourceField;
  }

  public DatasetEntity getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(DatasetEntity datasetId) {
    this.datasetId = datasetId;
  }
}
