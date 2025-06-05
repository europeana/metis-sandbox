package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;

/**
 * Represent errors or warnings in the dataset report
 */
@ApiModel("DatasetLog")
public record DatasetLogDTO(

    @JsonProperty("message")
    String message,
    Status type
) {

}
