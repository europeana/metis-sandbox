package eu.europeana.metis.sandbox.dto.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TiersZeroInfo}
 */
class TiersZeroInfoTest {

  @Test
  void getContentTierTest_expectSuccess() {
    TierStatistics contentTier = new TierStatistics(1, List.of("1"));
    TierStatistics metadataTier = new TierStatistics(2, List.of("2", "3"));
    TiersZeroInfo tiersZeroInfo = new TiersZeroInfo(contentTier, metadataTier);
    assertEquals(tiersZeroInfo.getContentTier(), contentTier);
  }

  @Test
  void getMetadataTierTest_expectSuccess() {
    TierStatistics contentTier = new TierStatistics(1, List.of("1"));
    TierStatistics metadataTier = new TierStatistics(2, List.of("2", "3"));
    TiersZeroInfo tiersZeroInfo = new TiersZeroInfo(contentTier, metadataTier);
    assertEquals(tiersZeroInfo.getMetadataTier(), metadataTier);
  }

  @Test
  void equalsTest_expectSuccess() {
    TierStatistics contentTier1 = new TierStatistics(1, List.of("1"));
    TierStatistics metadataTier1 = new TierStatistics(2, List.of("2", "3"));
    TierStatistics contentTier2 = new TierStatistics(3, List.of("1", "2", "3"));
    TierStatistics metadataTier2 = new TierStatistics(4, List.of("4", "5", "6", "7"));
    TiersZeroInfo tiersZeroInfo1 = new TiersZeroInfo(contentTier1, metadataTier1);
    TiersZeroInfo tiersZeroInfo2 = new TiersZeroInfo(contentTier1, metadataTier1);
    TiersZeroInfo tiersZeroInfo3 = new TiersZeroInfo(contentTier2, metadataTier2);
    assertEquals(tiersZeroInfo1, tiersZeroInfo1);
    assertEquals(tiersZeroInfo1, tiersZeroInfo2);
    assertNotEquals(tiersZeroInfo1, tiersZeroInfo3);
    assertNotEquals(tiersZeroInfo2, tiersZeroInfo3);
    assertNotEquals(tiersZeroInfo1, new TierStatistics(1, List.of("1")));
  }

  @Test
  void hashCodeTest_expectSuccess() {
    TierStatistics contentTier1 = new TierStatistics(1, List.of("1"));
    TierStatistics metadataTier1 = new TierStatistics(2, List.of("2", "3"));
    TierStatistics contentTier2 = new TierStatistics(3, List.of("1", "2", "3"));
    TierStatistics metadataTier2 = new TierStatistics(4, List.of("4", "5", "6", "7"));
    TiersZeroInfo tiersZeroInfo1 = new TiersZeroInfo(contentTier1, metadataTier1);
    TiersZeroInfo tiersZeroInfo2 = new TiersZeroInfo(contentTier1, metadataTier1);
    TiersZeroInfo tiersZeroInfo3 = new TiersZeroInfo(contentTier2, metadataTier2);
    assertEquals(tiersZeroInfo1.hashCode(), tiersZeroInfo2.hashCode());
    assertNotEquals(tiersZeroInfo1.hashCode(), tiersZeroInfo3.hashCode());
    assertNotEquals(tiersZeroInfo2.hashCode(), tiersZeroInfo3.hashCode());
  }
}
