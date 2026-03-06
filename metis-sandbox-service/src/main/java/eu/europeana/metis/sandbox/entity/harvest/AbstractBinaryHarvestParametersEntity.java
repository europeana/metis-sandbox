package eu.europeana.metis.sandbox.entity.harvest;

import static jakarta.persistence.FetchType.LAZY;

import eu.europeana.metis.sandbox.common.FileType;
import jakarta.persistence.Basic;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract class representing binary-based harvest parameters.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractBinaryHarvestParametersEntity extends HarvestParametersEntity {

  private String fileName;

  @Enumerated(EnumType.STRING)
  private FileType fileType;

  @Lob
  @Basic(fetch = LAZY)
  private byte[] fileContent;
}
