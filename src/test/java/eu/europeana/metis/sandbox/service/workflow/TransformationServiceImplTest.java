package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransformationServiceImplTest {

  private final TestUtils testUtils = new TestUtils();

  @Mock
  TransformXsltRepository transformXsltRepository;

  @Mock
  DatasetRepository datasetRepositoryMock;

  @Spy
  @InjectMocks
  TransformationServiceImpl transformationService;

  @Test
  void transform_expectSuccess() throws IOException{

    var input = testUtils.readFileToBytes(
            "record" + File.separator + "transform" + File.separator + "bnf" + File.separator + "bnf-record.xml");
    var expected = testUtils.readFileToBytes(
            "record" + File.separator + "transform" + File.separator + "bnf" + File.separator + "bnf-record-expected.xml");
    var transformFile = new ByteArrayInputStream(testUtils.readFileToBytes(
            "record" + File.separator +  "transform" + File.separator + "bnf" + File.separator + "BnF_Xslt_file.xslt"));

    var result = transformationService.transform("identifier", transformFile, input);

    assertArrayEquals(expected, result);

  }

  @Test
  void transform_emptyXml_expectRecordProcessingException() throws IOException {

    var transformFile = new ByteArrayInputStream(testUtils.readFileToBytes(
            "record" + File.separator +  "transform" + File.separator + "bnf" + File.separator + "BnF_Xslt_file.xslt"));

    RecordProcessingException exception =
            assertThrows(RecordProcessingException.class,
                    () -> transformationService.transform("identifier", transformFile, new byte[0]));

    assertEquals("identifier", exception.getRecordId());
  }

  @Test
  void transform_recordInput_expectSuccess() throws IOException {

    var input = testUtils.readFileToString(
            "record" + File.separator + "transform" + File.separator + "bnf" + File.separator + "bnf-record.xml");
    var expected = testUtils.readFileToString(
            "record" + File.separator + "transform" + File.separator + "bnf" + File.separator + "bnf-record-expected.xml");
    var transformFile = testUtils.readFileToString(
            "record" + File.separator +  "transform" + File.separator + "bnf" + File.separator + "BnF_Xslt_file.xslt");

    var inputRecord = createRecord(input);
    var expectedRecord = createRecord(expected);

    var expectedRecordInfo = new RecordInfo(expectedRecord, Collections.emptyList());

    when(datasetRepositoryMock.getXsltContentFromDatasetId(1)).thenReturn(transformFile);

    var result = transformationService.transform(inputRecord);

    assertEquals(expectedRecordInfo, result);

  }

  @Test
  void transformToEdmInternal_expectSuccess() throws IOException {

    var input = testUtils.readFileToString(
        "record" + File.separator + "transform" + File.separator + "record-input.xml");
    var expected = testUtils.readFileToString(
        "record" + File.separator + "transform" + File.separator + "record-expected.xml");
    var transformFile = testUtils.readFileToString(
        "record" + File.separator + "defaultTransform.xslt");

    var inputRecord = createRecord(input);
    var expectedRecord = createRecord(expected);

    var expectedRecordInfo = new RecordInfo(expectedRecord, Collections.emptyList());

    TransformXsltEntity transformXsltEntity = new TransformXsltEntity(transformFile);
    when(transformXsltRepository.findById(anyInt())).thenReturn(Optional.of(transformXsltEntity));

    var result = transformationService.transformToEdmInternal(inputRecord);

    assertEquals(expectedRecordInfo, result);

  }

  @Test
  void transformToEdmInternal_invalidXml_expectRecordProcessingException() throws IOException {

    var input = testUtils.readFileToString(
        "record" + File.separator + "record-missing-id.xml");

    var record = createRecord(input);

    RecordProcessingException exception =
        assertThrows(RecordProcessingException.class,
            () -> transformationService.transformToEdmInternal(record));

    assertEquals("1", exception.getRecordId());
  }

  private Record createRecord(String input){
    return Record.builder()
            .datasetId("1").datasetName("One")
            .providerId("1")
            .country(Country.ITALY)
            .language(Language.IT)
            .content(input.getBytes(StandardCharsets.UTF_8))
            .recordId(1L).build();
  }
}