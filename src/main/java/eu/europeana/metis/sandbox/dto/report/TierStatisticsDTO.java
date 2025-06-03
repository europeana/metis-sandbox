package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class to encapsulate the statistics information related to a tier
 *
 */
public class TierStatisticsDTO {

    @JsonProperty("total")
    private final int totalNumberOfRecords;
    @JsonProperty("samples")
    private final List<String> listRecordIds;

    /**
     * Constructor
     * @param totalNumberOfRecords The number of records that are associated to the tier
     * @param listRecordIds The ids of the record associated with the tier
     */
    public TierStatisticsDTO(int totalNumberOfRecords, List<String> listRecordIds) {
        this.totalNumberOfRecords = totalNumberOfRecords;
        this.listRecordIds = List.copyOf(listRecordIds);
    }

    public int getTotalNumberOfRecords() {
        return totalNumberOfRecords;
    }

    public List<String> getListRecordIds() {
        return listRecordIds.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(listRecordIds);
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }

        if(!(o instanceof TierStatisticsDTO other)){
            return false;
        }

      return this.totalNumberOfRecords == other.totalNumberOfRecords &&
                this.listRecordIds.equals(other.listRecordIds);
    }

    @Override
    public int hashCode(){
        return Objects.hash(totalNumberOfRecords, listRecordIds);
    }
}
