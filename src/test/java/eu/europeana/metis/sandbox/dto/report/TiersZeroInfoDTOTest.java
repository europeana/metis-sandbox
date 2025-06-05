package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TiersZeroInfoDTO}
 */
class TiersZeroInfoDTOTest {

  @Test
  void getContentTierTest_expectSuccess() {
    TierStatisticsDTO contentTier = new TierStatisticsDTO(1, List.of("1"));
    TierStatisticsDTO metadataTier = new TierStatisticsDTO(2, List.of("2", "3"));
    TiersZeroInfoDTO tiersZeroInfoDTO = new TiersZeroInfoDTO(contentTier, metadataTier);
    assertEquals(tiersZeroInfoDTO.contentTier(), contentTier);
  }

  @Test
  void getMetadataTierTest_expectSuccess() {
    TierStatisticsDTO contentTier = new TierStatisticsDTO(1, List.of("1"));
    TierStatisticsDTO metadataTier = new TierStatisticsDTO(2, List.of("2", "3"));
    TiersZeroInfoDTO tiersZeroInfoDTO = new TiersZeroInfoDTO(contentTier, metadataTier);
    assertEquals(tiersZeroInfoDTO.metadataTier(), metadataTier);
  }

  @Test
  void equalsTest_expectSuccess() {
    TierStatisticsDTO contentTier1 = new TierStatisticsDTO(1, List.of("1"));
    TierStatisticsDTO metadataTier1 = new TierStatisticsDTO(2, List.of("2", "3"));
    TierStatisticsDTO contentTier2 = new TierStatisticsDTO(3, List.of("1", "2", "3"));
    TierStatisticsDTO metadataTier2 = new TierStatisticsDTO(4, List.of("4", "5", "6", "7"));
    TiersZeroInfoDTO tiersZeroInfoDTO1 = new TiersZeroInfoDTO(contentTier1, metadataTier1);
    TiersZeroInfoDTO tiersZeroInfoDTO2 = new TiersZeroInfoDTO(contentTier1, metadataTier1);
    TiersZeroInfoDTO tiersZeroInfoDTO3 = new TiersZeroInfoDTO(contentTier2, metadataTier2);
    assertEquals(tiersZeroInfoDTO1, tiersZeroInfoDTO2);
    assertNotEquals(tiersZeroInfoDTO1, tiersZeroInfoDTO3);
    assertNotEquals(tiersZeroInfoDTO2, tiersZeroInfoDTO3);
    assertNotEquals(tiersZeroInfoDTO1, new TierStatisticsDTO(1, List.of("1")));
  }

  @Test
  void hashCodeTest_expectSuccess() {
    TierStatisticsDTO contentTier1 = new TierStatisticsDTO(1, List.of("1"));
    TierStatisticsDTO metadataTier1 = new TierStatisticsDTO(2, List.of("2", "3"));
    TierStatisticsDTO contentTier2 = new TierStatisticsDTO(3, List.of("1", "2", "3"));
    TierStatisticsDTO metadataTier2 = new TierStatisticsDTO(4, List.of("4", "5", "6", "7"));
    TiersZeroInfoDTO tiersZeroInfoDTO1 = new TiersZeroInfoDTO(contentTier1, metadataTier1);
    TiersZeroInfoDTO tiersZeroInfoDTO2 = new TiersZeroInfoDTO(contentTier1, metadataTier1);
    TiersZeroInfoDTO tiersZeroInfoDTO3 = new TiersZeroInfoDTO(contentTier2, metadataTier2);
    assertEquals(tiersZeroInfoDTO1.hashCode(), tiersZeroInfoDTO2.hashCode());
    assertNotEquals(tiersZeroInfoDTO1.hashCode(), tiersZeroInfoDTO3.hashCode());
    assertNotEquals(tiersZeroInfoDTO2.hashCode(), tiersZeroInfoDTO3.hashCode());
  }
}
