package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class DatasetGeneratorServiceImplTest {

  @Mock
  private RecordRepository recordRepository;

  @InjectMocks
  private DatasetGeneratorServiceImpl generator;

  @Test
  void generate_expectSuccess() {

    RecordEntity recordEntity1 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId1")
            .setProviderId("providerId1")
            .setDatasetId("1")
            .setContentTier("contentTier1")
            .setContentTierBeforeLicenseCorrection("contentTierBeforeLicenseCorrection1")
            .setMetadataTier("metadataTier1")
            .setMetadataTierLanguage("metadataTierLanguage1")
            .setMetadataTierEnablingElements("metadataTierEnablingElements1")
            .setMetadataTierContextualClasses("metadataTierContextualClasses1")
            .setLicense("license1").build();
    recordEntity1.setId(1L);
    RecordEntity recordEntity2 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId2")
            .setProviderId("providerId2")
            .setDatasetId("1")
            .setContentTier("contentTier2")
            .setContentTierBeforeLicenseCorrection("contentTierBeforeLicenseCorrection2")
            .setMetadataTier("metadataTier2")
            .setMetadataTierLanguage("metadataTierLanguage2")
            .setMetadataTierEnablingElements("metadataTierEnablingElements2")
            .setMetadataTierContextualClasses("metadataTierContextualClasses2")
            .setLicense("license2").build();
    recordEntity2.setId(2L);
    RecordEntity recordEntity3 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId3")
            .setProviderId("providerId3")
            .setDatasetId("1")
            .setContentTier("contentTier3")
            .setContentTierBeforeLicenseCorrection("contentTierBeforeLicenseCorrection3")
            .setMetadataTier("metadataTier3")
            .setMetadataTierLanguage("metadataTierLanguage3")
            .setMetadataTierEnablingElements("metadataTierEnablingElements3")
            .setMetadataTierContextualClasses("metadataTierContextualClasses3")
            .setLicense("license3").build();
    recordEntity3.setId(3L);
    RecordEntity recordEntity4 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId4")
            .setProviderId("providerId4")
            .setDatasetId("1")
            .setContentTier("contentTier4")
            .setContentTierBeforeLicenseCorrection("contentTierBeforeLicenseCorrection4")
            .setMetadataTier("metadataTier4")
            .setMetadataTierLanguage("metadataTierLanguage4")
            .setMetadataTierEnablingElements("metadataTierEnablingElements4")
            .setMetadataTierContextualClasses("metadataTierContextualClasses4")
            .setLicense("license4").build();
    recordEntity4.setId(4L);
    RecordEntity recordEntity5 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId5")
            .setProviderId("providerId5")
            .setDatasetId("1")
            .setContentTier("contentTier5")
            .setContentTierBeforeLicenseCorrection("contentTierBeforeLicenseCorrection5")
            .setMetadataTier("metadataTier5")
            .setMetadataTierLanguage("metadataTierLanguage5")
            .setMetadataTierEnablingElements("metadataTierEnablingElements5")
            .setMetadataTierContextualClasses("metadataTierContextualClasses5")
            .setLicense("license5").build();
    recordEntity5.setId(5L);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1, recordEntity2, recordEntity3, recordEntity4, recordEntity5);

    Dataset dataset = generator.generate(getTestDatasetMetadata(), getTestRecords());

    assertEquals(5, dataset.getRecords().size());
  }

  @Test
  void generateWithDuplicateRecord_expectSuccess() {

    RecordEntity recordEntity1 = new RecordEntity.RecordEntityBuilder()
            .setEuropeanaId("europeanaId1")
            .setProviderId("providerId1")
            .setDatasetId("1")
            .setContentTier("contentTier1")
            .setContentTierBeforeLicenseCorrection("contentTierBeforeLicenseCorrection1")
            .setMetadataTier("metadataTier1")
            .setMetadataTierLanguage("metadataTierLanguage1")
            .setMetadataTierEnablingElements("metadataTierEnablingElements1")
            .setMetadataTierContextualClasses("metadataTierContextualClasses1")
            .setLicense("license1").build();
    recordEntity1.setId(1L);

    when(recordRepository.save(any()))
        .thenReturn(recordEntity1);

    Dataset dataset = generator
        .generate(getTestDatasetMetadata(),
            List.of(new ByteArrayInputStream("record1".getBytes()),
                new ByteArrayInputStream("records".getBytes())));

    assertEquals(1, dataset.getRecords().size());
  }

  @Test
  void generate_emptyRecords_expectFail() {
    assertThrows(IllegalArgumentException.class, () -> generator.generate(getTestDatasetMetadata(), List.of()));
  }

  @Test
  void generate_inCaseOfInvalidRecords_expectSuccess() {
    final RecordEntity recordEntity1 = new RecordEntity.RecordEntityBuilder()
            .setDatasetId("1")
            .build();
    recordEntity1.setId(9514055L);
    final RecordEntity recordEntity2 = new RecordEntity.RecordEntityBuilder()
            .setDatasetId("1")
            .build();
    recordEntity1.setId(9514058L);
    when(recordRepository.save(any(RecordEntity.class))).thenThrow(RecordParsingException.class)
            .thenReturn(recordEntity1)
            .thenThrow(RecordParsingException.class)
            .thenThrow(IllegalArgumentException.class)
            .thenReturn(recordEntity2);

    Dataset dataset = generator.generate(getTestDatasetMetadata(), getTestRecords());

    assertEquals(2, dataset.getRecords().size());
  }

  private static DatasetMetadata getTestDatasetMetadata() {
    return DatasetMetadata.builder()
                          .withDatasetId("1")
                          .withDatasetName("datasetName")
                          .withCountry(Country.ITALY)
                          .withLanguage(Language.IT)
                          .build();
  }

  private static List<ByteArrayInputStream> getTestRecords() {
    return List.of(new ByteArrayInputStream("record1".getBytes()),
        new ByteArrayInputStream("record2".getBytes()),
        new ByteArrayInputStream("record3".getBytes()),
        new ByteArrayInputStream("record4".getBytes()),
        new ByteArrayInputStream("record5".getBytes()));
  }
}
