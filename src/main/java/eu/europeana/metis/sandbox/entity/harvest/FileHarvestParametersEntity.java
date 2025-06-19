package eu.europeana.metis.sandbox.entity.harvest;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents file-based harvest parameters.
 *
 * <p>Note: It does not contain any additional parameters and it is used as a "marker" class.
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("FILE")
@Table(name = "harvest_parameters_file")
public class FileHarvestParametersEntity extends BinaryHarvestParametersEntity {

}
