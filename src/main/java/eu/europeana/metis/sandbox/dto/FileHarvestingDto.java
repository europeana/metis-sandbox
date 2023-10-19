package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(FileHarvestingDto.SWAGGER_MODEL_NAME)
public class FileHarvestingDto implements HarvestingParametersDto {

    public static final String SWAGGER_MODEL_NAME = "FileUpload";

    @JsonProperty("protocol")
    private final String protocol = "FILE";

    @JsonProperty("file-name")
    private final String fileName;

    @JsonProperty("file-type")
    private final String fileType;

    public FileHarvestingDto(String fileName, String fileType) {
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public String getProtocol(){
        return protocol;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }
}
