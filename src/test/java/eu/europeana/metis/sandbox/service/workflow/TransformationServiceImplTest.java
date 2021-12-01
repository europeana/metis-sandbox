package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
  TransformationService transformationService =
      new TransformationServiceImpl(transformXsltRepository);

  // TODO: current tests fails since the actual xslt String is passed as argument,
  //  but in metis-transform library a String which is an URL is expected, check MET-4065, MET-4066

//  @Test
  void transform_expectSuccess() throws IOException  {
    var input = testUtils.readFileToString(
        "record" + File.separator + "transform" + File.separator + "record-input.xml");
    var expected = testUtils.readFileToString(
        "record" + File.separator + "transform" + File.separator + "record-expected.xml");
    var transformFile = testUtils.readFileToString(
        "record" + File.separator + "defaultTransform.xml");

    var record = Record.builder()
        .datasetId("1").datasetName("One")
        .country(Country.ITALY)
        .language(Language.IT)
        .content(input.getBytes(StandardCharsets.UTF_8))
        .recordId("1").build();
    var recordError = new RecordError("","");
    var recordInfo = new RecordInfo(record, List.of(recordError));

    TransformXsltEntity dummyEntity = new TransformXsltEntity(transformFile);
    when(transformXsltRepository.findById(anyInt())).thenReturn(Optional.of(dummyEntity));

    when(transformationService.transform(record)).thenReturn(recordInfo);

    var result = transformationService.transform(record);

    assertArrayEquals(expected.getBytes(), result.getRecord().getContent());
  }

  @Test
  void transform_nullRecord_expectNullPointerException() {
    assertThrows(NullPointerException.class, () -> transformationService.transform(null));
  }

//  @Test
  void transform_invalidXml_expectRecordProcessingException() throws IOException {

    var input = testUtils.readFileToBytes(
        "record" + File.separator + "bad-order" + File.separator + "record-input.xml");
    var transformFile = new String(
        testUtils.readFileToBytes("record" + File.separator + "defaultTransform.xml"),
        StandardCharsets.UTF_8);

    TransformXsltEntity dummyEntity = new TransformXsltEntity(transformFile);
    when(transformXsltRepository.findById(anyInt())).thenReturn(Optional.of(dummyEntity));

    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT).content(input)
        .recordId("1").build();

    when(transformationService.transform(record)).thenThrow(RecordProcessingException.class);

    RecordProcessingException exception = assertThrows(RecordProcessingException.class,
        () -> transformationService.transform(record));
    assertEquals("1", exception.getRecordId());
  }
}