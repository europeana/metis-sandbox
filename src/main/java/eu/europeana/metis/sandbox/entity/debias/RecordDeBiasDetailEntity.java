package eu.europeana.metis.sandbox.entity.debias;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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
 * Record debias detail entity.
 *
 * <p>Contains debias reports for tags in a record.
 */
@Getter
@Setter
@NoArgsConstructor
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
   * @param tagStart the tagStart
   * @param tagEnd the tagEnd
   * @param tagLength the tagLength
   * @param tagUri the tagUri
   */
  public RecordDeBiasDetailEntity(RecordDeBiasMainEntity debiasId, int tagStart, int tagEnd, int tagLength, String tagUri) {
    this.debiasId = debiasId;
    this.tagStart = tagStart;
    this.tagEnd = tagEnd;
    this.tagLength = tagLength;
    this.tagUri = tagUri;
  }
}
