package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TierStatisticsDTO}
 */
class TierStatisticsDTOTest {

  @Test
  void getTotalNumberOfRecordsTest_expectSuccess() {
    TierStatisticsDTO tierStatisticsDTO = new TierStatisticsDTO(2, List.of("1", "2"));
    assertEquals(2, tierStatisticsDTO.totalNumberOfRecords());
  }

  @Test
  void getListRecordIdsTest_expectSuccess() {
    TierStatisticsDTO tierStatisticsDTO = new TierStatisticsDTO(2, List.of("1", "2"));
    assertEquals(tierStatisticsDTO.recordIds(), List.of("1", "2"));
  }

  @Test
  void equalsTest_expectSuccess() {
    TierStatisticsDTO tierStatisticsDTO1 = new TierStatisticsDTO(2, List.of("1", "2"));
    TierStatisticsDTO tierStatisticsDTO2 = new TierStatisticsDTO(2, List.of("1", "2"));
    TierStatisticsDTO tierStatisticsDTO3 = new TierStatisticsDTO(4, List.of("1", "2", "3", "4"));
    assertEquals(tierStatisticsDTO1, tierStatisticsDTO2);
    assertNotEquals(tierStatisticsDTO1, tierStatisticsDTO3);
    assertNotEquals(tierStatisticsDTO2, tierStatisticsDTO3);
  }

  @Test
  void hashCodeTest_expectSuccess() {
    TierStatisticsDTO tierStatisticsDTO1 = new TierStatisticsDTO(2, List.of("1", "2"));
    TierStatisticsDTO tierStatisticsDTO2 = new TierStatisticsDTO(2, List.of("1", "2"));
    TierStatisticsDTO tierStatisticsDTO3 = new TierStatisticsDTO(4, List.of("1", "2", "3", "4"));
    assertEquals(tierStatisticsDTO1.hashCode(), tierStatisticsDTO2.hashCode());
    assertNotEquals(tierStatisticsDTO1.hashCode(), tierStatisticsDTO3.hashCode());
    assertNotEquals(tierStatisticsDTO2.hashCode(), tierStatisticsDTO3.hashCode());
  }

}
