package eu.europeana.metis.sandbox.dto.report;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link TierStatistics}
 */
class TierStatisticsTest {

    @Test
    void getTotalNumberOfRecordsTest_expectSuccess(){
        TierStatistics tierStatistics = new TierStatistics(2, List.of("1","2"));
        assertEquals(tierStatistics.getTotalNumberOfRecords(), 2);
    }

    @Test
    void getListRecordIdsTest_expectSuccess(){
        TierStatistics tierStatistics = new TierStatistics(2, List.of("1","2"));
        assertEquals(tierStatistics.getListRecordIds(), List.of("1", "2"));
    }

    @Test
    void equalsTest_expectSuccess(){
        TierStatistics tierStatistics1 = new TierStatistics(2, List.of("1","2"));
        TierStatistics tierStatistics2 = new TierStatistics(2, List.of("1","2"));
        TierStatistics tierStatistics3 = new TierStatistics(4, List.of("1","2","3","4"));
        assertTrue(tierStatistics1.equals(tierStatistics1));
        assertTrue(tierStatistics1.equals(tierStatistics2));
        assertFalse(tierStatistics1.equals(tierStatistics3));
        assertFalse(tierStatistics2.equals(tierStatistics3));
        assertFalse(tierStatistics1.equals(new TiersZeroInfo(tierStatistics1, tierStatistics2)));
    }

    @Test
    void hashCodeTest_expectSuccess(){
        TierStatistics tierStatistics1 = new TierStatistics(2, List.of("1","2"));
        TierStatistics tierStatistics2 = new TierStatistics(2, List.of("1","2"));
        TierStatistics tierStatistics3 = new TierStatistics(4, List.of("1","2","3","4"));
        assertEquals(tierStatistics1.hashCode(), tierStatistics2.hashCode());
        assertNotEquals(tierStatistics1.hashCode(), tierStatistics3.hashCode());
        assertNotEquals(tierStatistics2.hashCode(), tierStatistics3.hashCode());
    }

}
