package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.service.util.XmlRecordProcessorService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultDatasetGeneratorTest {

  @Mock
  private XmlRecordProcessorService xmlRecordProcessorService;

  @InjectMocks
  private DefaultDatasetGenerator generator;

  @Test
  void generate_expectSuccess() {

    when(xmlRecordProcessorService.getRecordId(anyString())).thenReturn("id");

    var dataset = generator
        .generate("name", Country.ITALY, Language.IT, List.of("record1", "record"));

    assertEquals(2, dataset.getRecords().size());
  }

  @Test
  void generate_emptyRecords_expectFail() {
    assertThrows(IllegalArgumentException.class, () -> generator
        .generate("name", Country.ITALY, Language.IT, List.of()));
  }
}