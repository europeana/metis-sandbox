package eu.europeana.metis.sandbox.entity.harvest;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("HTTP")
@Table(name = "harvest_parameters_http")
public class HttpHarvestParameters extends HarvestParametersEntity {

  private String url;
  private String fileName;
  private String fileType; // ZIP, GZIP
  @Lob
  private byte[] fileContent;

}

