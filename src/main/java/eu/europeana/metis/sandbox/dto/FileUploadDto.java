package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(FileUploadDto.SWAGGER_MODEL_NAME)
public class FileUploadDto implements UploadDataDto{

    public static final String SWAGGER_MODEL_NAME = "FileUpload";

    @JsonProperty("protocol")
    private final String protocol = "FILE";

    @JsonProperty("file-name")
    private final String fileName;

    @JsonProperty("file-type")
    private final String fileType;

    public FileUploadDto(String fileName, String fileType) {
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
