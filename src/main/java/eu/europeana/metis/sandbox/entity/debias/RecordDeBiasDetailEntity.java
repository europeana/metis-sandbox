package eu.europeana.metis.sandbox.entity.debias;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordEntity;
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

  protected int tag_start;

  protected int tag_end;

  protected int tag_length;

  protected String tag_uri;

  public RecordDeBiasDetailEntity(RecordDeBiasMainEntity debiasId, int start, int end, int length, String uri) {
    this.debiasId = debiasId;
    this.tag_start = start;
    this.tag_end = end;
    this.tag_length = length;
    this.tag_uri = uri;
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

  public int getTag_start() {
    return tag_start;
  }

  public void setTag_start(int tag_start) {
    this.tag_start = tag_start;
  }

  public int getTag_end() {
    return tag_end;
  }

  public void setTag_end(int tag_end) {
    this.tag_end = tag_end;
  }

  public int getTag_length() {
    return tag_length;
  }

  public void setTag_length(int tag_length) {
    this.tag_length = tag_length;
  }

  public String getTag_uri() {
    return tag_uri;
  }

  public void setTag_uri(String tag_uri) {
    this.tag_uri = tag_uri;
  }
}
