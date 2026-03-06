package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Class to encapsulate the statistics information related to a tier
 */
public record TierStatisticsDTO(

    @JsonProperty("total")
    int totalNumberOfRecords,
    @JsonProperty("samples")
    List<String> recordIds
) {

}
