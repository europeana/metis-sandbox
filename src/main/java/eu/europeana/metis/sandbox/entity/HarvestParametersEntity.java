package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.HarvestProtocol;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity to map harvesting_parameters table
 */
@Setter
@Getter
@Entity
@Table(name = "harvest_parameters")
public class HarvestParametersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
    private DatasetEntity datasetId;

    @Enumerated(EnumType.STRING)
    private HarvestProtocol harvestProtocol;

    private String fileName;

    private String fileType;

    @Lob
    private byte[] fileContent;

    private String url;

    private String setSpec;

    private String metadataFormat;
}
