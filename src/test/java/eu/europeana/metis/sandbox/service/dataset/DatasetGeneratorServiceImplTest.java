package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
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
  private XmlRecordProcessorService xmlRecordProcessorService;

  @InjectMocks
  private DatasetGeneratorServiceImpl generator;

  @Test
  void generate_expectSuccess() {

    when(xmlRecordProcessorService.getProviderId(any(byte[].class)))
        .thenReturn("1")
        .thenReturn("2");

    var dataset = generator
        .generate("1", "name", Country.ITALY, Language.IT,
            List.of(new ByteArrayInputStream("record1".getBytes()),
                new ByteArrayInputStream("records".getBytes())));

    assertEquals(2, dataset.getRecords().size());
  }

  @Test
  void generateWithDuplicateRecord_expectSuccess() {

    when(xmlRecordProcessorService.getProviderId(any(byte[].class)))
        .thenReturn("1");

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