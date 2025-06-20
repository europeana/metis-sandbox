package eu.europeana.metis.sandbox.entity.harvest;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents HTTP-specific harvest parameters.
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("HTTP")
@Table(name = "harvest_parameters_http")
public class HttpHarvestParametersEntity extends AbstractBinaryHarvestParametersEntity {

  private String url;
}

