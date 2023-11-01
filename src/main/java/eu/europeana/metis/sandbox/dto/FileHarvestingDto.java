package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Protocol;
import io.swagger.annotations.ApiModel;

/**
 * Represents the data related to File harvesting
 */
@ApiModel(FileHarvestingDto.SWAGGER_MODEL_NAME)
public class FileHarvestingDto implements HarvestingParametersDto {

    public static final String SWAGGER_MODEL_NAME = "FileUpload";

    @JsonProperty("protocol")
    private final Protocol protocol = Protocol.FILE;

    @JsonProperty("file-name")
    private final String fileName;

    @JsonProperty("file-type")
    private final String fileType;

    /**
     * Constructor of this Dto
     * @param fileName The name of the file used for harvesting
     * @param fileType The name of file uploaded (e.g., zip, tar,...)
     */
    public FileHarvestingDto(String fileName, String fileType) {
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public Protocol getProtocol(){
        return protocol;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }
}
