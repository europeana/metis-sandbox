package eu.europeana.metis.sandbox.entity.harvest;

import eu.europeana.metis.sandbox.entity.DatasetEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents harvest parameters for datasets. It acts as a base class for specific types of harvest parameters.
 *
 * <p>It uses database inheritance with {@link Inheritance}.
 * <p>Classes extending this need to specify {@link DiscriminatorValue} to activate inheritance handling.
 */
@Getter
@Setter
@Entity
@Table(name = "harvest_parameters")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "harvest_protocol", discriminatorType = DiscriminatorType.STRING)
public class HarvestParametersEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
  private DatasetEntity datasetEntity;

  private Integer stepSize;
}
