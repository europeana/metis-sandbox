package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.dto.FileHarvestingDto;
import eu.europeana.metis.sandbox.dto.HttpHarvestingDto;
import eu.europeana.metis.sandbox.dto.OAIPmhHarvestingDto;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;

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

    /**
     * Constructor based on parametricDto type FileHarvestingDto
     * @param datasetId The id of the dataset associated to this type of harvesting
     * @param fileHarvestingDto The object encapsulating all data related to file harvesting
     */
    public HarvestingParameterEntity(DatasetEntity datasetId, FileHarvestingDto fileHarvestingDto){
        this.datasetId = datasetId;
        this.harvestProtocol = fileHarvestingDto.getProtocol();
        this.fileName = fileHarvestingDto.getFileName();
        this.fileType = fileHarvestingDto.getFileType();
        this.url = null;
        this.setSpec = null;
        this.metadataFormat = null;
    }

    /**
     * Constructor based on parametricDto type HttpHarvestingDto
     * @param datasetId The id of the dataset associated to this type of harvesting
     * @param httpHarvestingDto The object encapsulating all data related to http harvesting
     */
    public HarvestingParameterEntity(DatasetEntity datasetId, HttpHarvestingDto httpHarvestingDto){
        this.datasetId = datasetId;
        this.harvestProtocol = httpHarvestingDto.getProtocol();
        this.fileName = null;
        this.fileType = null;
        this.url = httpHarvestingDto.getUrl();
        this.setSpec = null;
        this.metadataFormat = null;
    }

    /**
     * Constructor based on parametricDto type OAIPmhHarvestingDto
     * @param datasetId The id of the dataset associated to this type of harvesting
     * @param oaiPmhHarvestingDto The object encapsulating all data related to OAI-PMH harvesting
     */
    public HarvestingParameterEntity(DatasetEntity datasetId, OAIPmhHarvestingDto oaiPmhHarvestingDto){
        this.datasetId = datasetId;
        this.harvestProtocol = oaiPmhHarvestingDto.getProtocol();
        this.fileName = null;
        this.fileType = null;
        this.url = oaiPmhHarvestingDto.getUrl();
        this.setSpec = oaiPmhHarvestingDto.getSetSpec();
        this.metadataFormat = oaiPmhHarvestingDto.getMetadataFormat();
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
