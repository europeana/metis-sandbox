package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import io.swagger.annotations.ApiModel;

/**
 * Represents the data related to File harvesting
 */
@ApiModel(FileHarvestingDTO.SWAGGER_MODEL_NAME)
public class FileHarvestingDTO implements HarvestingParametersDTO {

    public static final String SWAGGER_MODEL_NAME = "FileUpload";

    @JsonProperty("harvest-protocol")
    private final String harvestProtocol = HarvestProtocol.FILE.value();

    @JsonProperty("file-name")
    private final String fileName;

    @JsonProperty("file-type")
    private final String fileType;

    /**
     * Constructor of this Dto
     * @param fileName The name of the file used for harvesting
     * @param fileType The name of file uploaded (e.g., zip, tar,...)
     */
    public FileHarvestingDTO(String fileName, String fileType) {
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public HarvestProtocol getHarvestProtocol(){
        return HarvestProtocol.FILE;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }
}
