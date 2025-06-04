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
@DiscriminatorValue("FILE")
@Table(name = "harvest_parameters_file")
public class FileHarvestParameters extends HarvestParametersEntity {

  private String fileName;
  private String fileType; // ZIP, GZIP, XML
  @Lob
  private byte[] fileContent;
}

