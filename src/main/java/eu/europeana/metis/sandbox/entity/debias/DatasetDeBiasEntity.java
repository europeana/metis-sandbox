package eu.europeana.metis.sandbox.entity.debias;

import eu.europeana.metis.sandbox.dto.debias.DebiasState;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dataset debias entity representing its state.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "dataset_debias_detect")
public class DatasetDeBiasEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(cascade = CascadeType.MERGE)
  @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
  private DatasetEntity datasetId;

  private DebiasState debiasState;

  private ZonedDateTime createdDate;

  /**
   * Constructor.
   *
   * @param datasetId the dataset entity associated with the debiasing process
   * @param debiasState the current state of the debiasing process
   * @param createdDate the timestamp when the debiasing process was created
   */
  public DatasetDeBiasEntity(DatasetEntity datasetId, DebiasState debiasState, ZonedDateTime createdDate) {
    this.datasetId = datasetId;
    this.debiasState = debiasState;
    this.createdDate = createdDate;
  }
}
