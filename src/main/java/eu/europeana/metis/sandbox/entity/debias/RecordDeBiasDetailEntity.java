package eu.europeana.metis.sandbox.entity.debias;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entity to map to record_log table
 */
@Entity
@Table(name = "record_debias_detail")
public class RecordDeBiasDetailEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "debias_id", referencedColumnName = "id")
  private RecordDeBiasMainEntity debiasId;

  /**
   * The Tag start.
   */
  protected int tagStart;
  /**
   * The Tag end.
   */
  protected int tagEnd;
  /**
   * The Tag length.
   */
  protected int tagLength;
  /**
   * The Tag uri.
   */
  protected String tagUri;

  /**
   * Instantiates a new Record de bias detail entity.
   *
   * @param debiasId the debias id
   * @param start the start
   * @param end the end
   * @param length the length
   * @param uri the uri
   */
  public RecordDeBiasDetailEntity(RecordDeBiasMainEntity debiasId, int start, int end, int length, String uri) {
    this.debiasId = debiasId;
    this.tagStart = start;
    this.tagEnd = end;
    this.tagLength = length;
    this.tagUri = uri;
  }

  /**
   * Instantiates a new Record de bias detail entity.
   */
  public RecordDeBiasDetailEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
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
   * Gets debias id.
   *
   * @return the debias id
   */
  public RecordDeBiasMainEntity getDebiasId() {
    return debiasId;
  }

  /**
   * Sets debias id.
   *
   * @param debiasId the debias id
   */
  public void setDebiasId(RecordDeBiasMainEntity debiasId) {
    this.debiasId = debiasId;
  }

  /**
   * Gets tag start.
   *
   * @return the tag start
   */
  public int getTagStart() {
    return tagStart;
  }

  /**
   * Sets tag start.
   *
   * @param tagStart the tag start
   */
  public void setTagStart(int tagStart) {
    this.tagStart = tagStart;
  }

  /**
   * Gets tag end.
   *
   * @return the tag end
   */
  public int getTagEnd() {
    return tagEnd;
  }

  /**
   * Sets tag end.
   *
   * @param tagEnd the tag end
   */
  public void setTagEnd(int tagEnd) {
    this.tagEnd = tagEnd;
  }

  /**
   * Gets tag length.
   *
   * @return the tag length
   */
  public int getTagLength() {
    return tagLength;
  }

  /**
   * Sets tag length.
   *
   * @param tagLength the tag length
   */
  public void setTagLength(int tagLength) {
    this.tagLength = tagLength;
  }

  /**
   * Gets tag uri.
   *
   * @return the tag uri
   */
  public String getTagUri() {
    return tagUri;
  }

  /**
   * Sets tag uri.
   *
   * @param tagUri the tag uri
   */
  public void setTagUri(String tagUri) {
    this.tagUri = tagUri;
  }
}
