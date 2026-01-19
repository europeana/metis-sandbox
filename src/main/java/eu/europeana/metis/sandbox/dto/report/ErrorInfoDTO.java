package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Represent errors in the dataset report
 */
@Schema(name = "ErrorInfo")
public record ErrorInfoDTO(

    @JsonProperty("message")
    String errorMessage,
    Status type,

    @JsonProperty("records")
    List<String> recordIds
) {

}
