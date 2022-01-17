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

    RecordEntity recordEntity1 = new RecordEntity("europeanaId1", "providerId1", "1", "record1");
    recordEntity1.setId(1L);
    RecordEntity recordEntity2 = new RecordEntity("europeanaId2", "providerId2", "1", "record2");
    recordEntity2.setId(2L);
    RecordEntity recordEntity3 = new RecordEntity("europeanaId3", "providerId3", "1", "record3");
    recordEntity2.setId(3L);
    RecordEntity recordEntity4 = new RecordEntity("europeanaId4", "providerId4", "1", "record4");
    recordEntity2.setId(4L);
    RecordEntity recordEntity5 = new RecordEntity("europeanaId5", "providerId5", "1", "record5");
    recordEntity2.setId(5L);
    when(recordRepository.save(any()))
            .thenReturn(recordEntity1)
            .thenReturn(recordEntity2)
            .thenReturn(recordEntity3)
            .thenReturn(recordEntity4)
            .thenReturn(recordEntity5);

    Dataset dataset = generator.generate(getTestDatasetMetadata(), getTestRecords());

    assertEquals(5, dataset.getRecords().size());
  }

  @Test
  void generateWithDuplicateRecord_expectSuccess() {

    RecordEntity recordEntity1 = new RecordEntity("europeanaId1", "providerId1", "1", "record1");
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

//  @Test
//  void generate_inCaseOfInvalidRecords_expectSuccess() {
//    when(xmlRecordProcessorService.getRecordId("record1".getBytes())).thenThrow(RecordParsingException.class);
//    when(xmlRecordProcessorService.getRecordId("record2".getBytes())).thenReturn("2");
//    when(xmlRecordProcessorService.getRecordId("record3".getBytes())).thenThrow(RecordParsingException.class);
//    when(xmlRecordProcessorService.getRecordId("record4".getBytes())).thenThrow(IllegalArgumentException.class);
//    when(xmlRecordProcessorService.getRecordId("record5".getBytes())).thenReturn("5");
//
//    Dataset dataset = generator.generate(getTestDatasetMetadata(), getTestRecords());
//
//    assertEquals(2, dataset.getRecords().size());
//  }

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