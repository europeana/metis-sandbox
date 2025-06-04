package eu.europeana.metis.sandbox.entity.harvest;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("OAI")
@Table(name = "harvest_parameters_oai")
public class OaiHarvestParameters extends HarvestParametersEntity {

  private String url;
  private String setSpec;
  private String metadataFormat;
}
