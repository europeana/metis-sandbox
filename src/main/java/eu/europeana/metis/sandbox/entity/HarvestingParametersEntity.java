package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.Protocol;

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

@Entity
@Table(name = "harvesting_parameters")
public class HarvestingParametersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dataset_id", referencedColumnName = "datasetId")
    private DatasetEntity datasetId;

    @Enumerated(EnumType.STRING)
    private Protocol protocol;

    private String fileName;

    private String fileType;

    private String url;

    private String setSpec;

    private String metadataFormat;

    public HarvestingParametersEntity(DatasetEntity datasetId, Protocol protocol, String fileName, String fileType, String url, String setSpec, String metadataFormat) {
        this.datasetId = datasetId;
        this.protocol = protocol;
        this.fileName = fileName;
        this.fileType = fileType;
        this.url = url;
        this.setSpec = setSpec;
        this.metadataFormat = metadataFormat;
    }

    public HarvestingParametersEntity() {
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

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
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
