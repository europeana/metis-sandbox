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

  protected int tagStart;
  protected int tagEnd;
  protected int tagLength;
  protected String tagUri;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "debias_id", referencedColumnName = "id")
  private RecordDeBiasMainEntity debiasId;

  public RecordDeBiasDetailEntity(RecordDeBiasMainEntity debiasId, int start, int end, int length, String uri) {
    this.debiasId = debiasId;
    this.tagStart = start;
    this.tagEnd = end;
    this.tagLength = length;
    this.tagUri = uri;
  }

  public RecordDeBiasDetailEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public RecordDeBiasMainEntity getDebiasId() {
    return debiasId;
  }

  public void setDebiasId(RecordDeBiasMainEntity debiasId) {
    this.debiasId = debiasId;
  }

  public int getTagStart() {
    return tagStart;
  }

  public void setTagStart(int tagStart) {
    this.tagStart = tagStart;
  }

  public int getTagEnd() {
    return tagEnd;
  }

  public void setTagEnd(int tagEnd) {
    this.tagEnd = tagEnd;
  }

  public int getTagLength() {
    return tagLength;
  }

  public void setTagLength(int tagLength) {
    this.tagLength = tagLength;
  }

  public String getTagUri() {
    return tagUri;
  }

  public void setTagUri(String tagUri) {
    this.tagUri = tagUri;
  }
}
