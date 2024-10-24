package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TierStatistics}
 */
class TierStatisticsTest {

  @Test
  void getTotalNumberOfRecordsTest_expectSuccess() {
    TierStatistics tierStatistics = new TierStatistics(2, List.of("1", "2"));
    assertEquals(2, tierStatistics.getTotalNumberOfRecords());
  }

  @Test
  void getListRecordIdsTest_expectSuccess() {
    TierStatistics tierStatistics = new TierStatistics(2, List.of("1", "2"));
    assertEquals(tierStatistics.getListRecordIds(), List.of("1", "2"));
  }

  @Test
  void equalsTest_expectSuccess() {
    TierStatistics tierStatistics1 = new TierStatistics(2, List.of("1", "2"));
    TierStatistics tierStatistics2 = new TierStatistics(2, List.of("1", "2"));
    TierStatistics tierStatistics3 = new TierStatistics(4, List.of("1", "2", "3", "4"));
    assertEquals(tierStatistics1, tierStatistics1);
    assertEquals(tierStatistics1, tierStatistics2);
    assertNotEquals(tierStatistics1, tierStatistics3);
    assertNotEquals(tierStatistics2, tierStatistics3);
    assertNotEquals(tierStatistics1, new TiersZeroInfo(tierStatistics1, tierStatistics2));
  }

  @Test
  void hashCodeTest_expectSuccess() {
    TierStatistics tierStatistics1 = new TierStatistics(2, List.of("1", "2"));
    TierStatistics tierStatistics2 = new TierStatistics(2, List.of("1", "2"));
    TierStatistics tierStatistics3 = new TierStatistics(4, List.of("1", "2", "3", "4"));
    assertEquals(tierStatistics1.hashCode(), tierStatistics2.hashCode());
    assertNotEquals(tierStatistics1.hashCode(), tierStatistics3.hashCode());
    assertNotEquals(tierStatistics2.hashCode(), tierStatistics3.hashCode());
  }

}
