package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Class to encapsulate the statistics information related to a tier
 *
 */
public class TierStatistics {

    @JsonProperty("total")
    private int totalNumberOfRecords;
    @JsonProperty("samples")
    private List<String> listRecordIds;

    /**
     * Constructor
     * @param totalNumberOfRecords The number of records that are associated to the tier
     * @param listRecordIds The ids of the record associated with the tier
     */
    public TierStatistics(int totalNumberOfRecords, List<String> listRecordIds) {
        this.totalNumberOfRecords = totalNumberOfRecords;
        this.listRecordIds = listRecordIds;
    }

    public int getTotalNumberOfRecords() {
        return totalNumberOfRecords;
    }

    public List<String> getListRecordIds() {
        return listRecordIds.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(listRecordIds);
    }
}
