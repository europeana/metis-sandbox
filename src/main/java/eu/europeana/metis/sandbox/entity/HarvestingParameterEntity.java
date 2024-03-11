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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entity to map harvesting_parameters table
 */
@Entity
@Table(name = "harvesting_parameter")
public class HarvestingParameterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
    private DatasetEntity datasetId;

    @Enumerated(EnumType.STRING)
    private HarvestProtocol harvestProtocol;

    private String fileName;

    private String fileType;

    private String url;

    private String setSpec;

    private String metadataFormat;

    /**
     * Parameterized constructor
     * @param datasetId The id of the dataset associated to these harvesting parameters
     * @param harvestProtocol The type of harvesting done
     * @param fileName The name of ile that was harvested (if it exists)
     * @param fileType The type of the file that was harvested (if it exists)
     * @param url The url used to be harvested (if it exists)
     * @param setSpec The setspec used for the harvesting (if it exists)
     * @param metadataFormat The metadata format used for the harvesting (if it exists)
     */
    public HarvestingParameterEntity(DatasetEntity datasetId, HarvestProtocol harvestProtocol, String fileName, String fileType,
                                     String url, String setSpec, String metadataFormat) {
        this.datasetId = datasetId;
        this.harvestProtocol = harvestProtocol;
        this.fileName = fileName;
        this.fileType = fileType;
        this.url = url;
        this.setSpec = setSpec;
        this.metadataFormat = metadataFormat;
    }

    public HarvestingParameterEntity() {
        // provide explicit no-args constructor as it is required for Hibernate
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DatasetEntity getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(DatasetEntity datasetId) {
        this.datasetId = datasetId;
    }

    public HarvestProtocol getProtocol() {
        return harvestProtocol;
    }

    public void setProtocol(HarvestProtocol harvestProtocol) {
        this.harvestProtocol = harvestProtocol;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSetSpec() {
        return setSpec;
    }

    public void setSetSpec(String setSpec) {
        this.setSpec = setSpec;
    }

    public String getMetadataFormat() {
        return metadataFormat;
    }

    public void setMetadataFormat(String metadataFormat) {
        this.metadataFormat = metadataFormat;
    }
}
