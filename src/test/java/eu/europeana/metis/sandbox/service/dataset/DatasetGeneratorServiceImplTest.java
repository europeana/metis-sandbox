package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
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
    when(recordRepository.save(any()))
        .thenReturn(recordEntity1)
        .thenReturn(recordEntity2);

    var dataset = generator
        .generate("1", "name", Country.ITALY, Language.IT,
            List.of(new ByteArrayInputStream("record1".getBytes()),
                new ByteArrayInputStream("records".getBytes())));

    assertEquals(2, dataset.getRecords().size());
  }

  @Test
  void generateWithDuplicateRecord_expectSuccess() {

    RecordEntity recordEntity1 = new RecordEntity("europeanaId1", "providerId1", "1", "record1");
    recordEntity1.setId(1L);

    when(recordRepository.save(any()))
        .thenReturn(recordEntity1);

    var dataset = generator
        .generate("1", "name", Country.ITALY, Language.IT,
            List.of(new ByteArrayInputStream("record1".getBytes()),
                new ByteArrayInputStream("records".getBytes())));

    assertEquals(1, dataset.getRecords().size());
  }

  @Test
  void generate_emptyRecords_expectFail() {
    assertThrows(IllegalArgumentException.class, () -> generator
        .generate("1", "name", Country.ITALY, Language.IT, List.of()));
  }
}