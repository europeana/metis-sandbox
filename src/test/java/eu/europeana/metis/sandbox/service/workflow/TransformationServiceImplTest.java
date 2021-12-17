package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransformationServiceImplTest {

  private final TestUtils testUtils = new TestUtils();

  @Mock
  TransformXsltRepository transformXsltRepository;

  @InjectMocks
  TransformationServiceImpl transformationService;

  @Test
  void transformationService_expectSuccess() throws IOException {

    var input = testUtils.readFileToString(
        "record" + File.separator + "transform" + File.separator + "record-input.xml");
    var expected = testUtils.readFileToString(
        "record" + File.separator + "transform" + File.separator + "record-expected.xml");
    var transformFile = testUtils.readFileToString(
        "record" + File.separator + "defaultTransform.xslt");

    var inputRecord = Record.builder()
        .datasetId("1").datasetName("One")
        .country(Country.ITALY)
        .language(Language.IT)
        .content(input.getBytes(StandardCharsets.UTF_8))
        .recordId("1").build();

    var expectedRecord = Record.builder()
        .datasetId("1").datasetName("One")
        .country(Country.ITALY)
        .language(Language.IT)
        .content(expected.getBytes(StandardCharsets.UTF_8))
        .recordId("1").build();

    var expectedRecordInfo = new RecordInfo(expectedRecord, Collections.emptyList());

    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(transformFile);
    when(transformXsltRepository.findById(anyInt())).thenReturn(Optional.of(transformXsltEntity));

    var result = transformationService.transform(inputRecord);

    assertEquals(expectedRecordInfo, result);

  }

  @Test
  void transformationService_invalidXml_expectRecordProcessingException() throws IOException {

    var input = testUtils.readFileToString(
        "record" + File.separator + "record-missing-id.xml");

    var record = Record.builder()
        .datasetId("1").datasetName("One")
        .country(Country.ITALY)
        .language(Language.IT)
        .content(input.getBytes(StandardCharsets.UTF_8))
        .recordId("1").build();

    RecordProcessingException exception =
        assertThrows(RecordProcessingException.class,
            () -> transformationService.transform(record));

    assertEquals("1", exception.getRecordId());
  }
}