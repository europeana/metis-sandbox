package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;


/**
 * Represent dataset errors in the dataset report
 */
@ApiModel("DatasetErrorInfo")
public record DatasetErrorInfoDTO(

    @JsonProperty("message")
    String errorMessage,
    Status type
) {

}
