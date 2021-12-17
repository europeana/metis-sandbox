package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class TransformationServiceImplTest {

  private final TestUtils testUtils = new TestUtils();

  @Mock
  private ObjectProvider<XsltTransformer> objectProvider;

  @Mock
  private XsltTransformer xsltTransformer;

  @InjectMocks
  @Spy
  private TransformationServiceImpl service;

  @Test
  void transform_expectSuccess() throws IOException, TransformationException {
    var recordContent = testUtils.readFileToBytes("record"+ File.separator+"transform"+File.separator+"record-input.xml");
    var expected = testUtils.readFileToString("record"+File.separator+"transform"+File.separator+"record-expected.xml");

    ByteArrayInputStream xsltContent = new ByteArrayInputStream(
        Files.readAllBytes(Paths.get("src/test/resources/record/xsltFileContent.xsl")));

    when(service.getNewTransformerObject("identifier", xsltContent)).thenReturn(xsltTransformer);
    when(xsltTransformer.transformToBytes(recordContent, null))
        .thenReturn(expected.getBytes());

    var result = service.transform("identifier", xsltContent, recordContent);

    assertArrayEquals(expected.getBytes(), result);
  }

  @Test
  void transformToEdmInternal_expectSuccess() throws IOException, TransformationException {
    var input = testUtils.readFileToBytes("record"+ File.separator+"transform"+File.separator+"record-input.xml");
    var expected = testUtils.readFileToString("record"+File.separator+"transform"+File.separator+"record-expected.xml");

    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT).content(input)
        .recordId("").build();

    when(objectProvider.getObject(anyString(), anyString(), anyString()))
        .thenReturn(xsltTransformer);
    when(xsltTransformer.transformToBytes(any(byte[].class), any(EuropeanaGeneratedIdsMap.class)))
        .thenReturn(expected.getBytes());

    var result = service.transformToEdmInternal(record);

    assertArrayEquals(expected.getBytes(), result.getRecord().getContent());
  }

  @Test
  void transform_nullRecord_expectFail() throws IOException {
    ByteArrayInputStream xsltContent = new ByteArrayInputStream(
        Files.readAllBytes(Paths.get("src/test/resources/record/xsltFileContent.xsl")));
    assertThrows(NullPointerException.class, () -> service.transform("identifier", xsltContent, null));
  }

  @Test
  void transform_nullXsltContent_expectFail() {
    assertThrows(RecordProcessingException.class, () -> service.transform("identifier", null, "record".getBytes(
        StandardCharsets.UTF_8)));
  }

  @Test
  void transform_nullIdentifier_expectFail() throws IOException {
    ByteArrayInputStream xsltContent = new ByteArrayInputStream(
        Files.readAllBytes(Paths.get("src/test/resources/record/xsltFileContent.xsl")));
    assertThrows(RecordProcessingException.class, () -> service.transform(null, xsltContent, "record".getBytes(
        StandardCharsets.UTF_8)));
  }

  @Test
  void transformToEdmInternal_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.transformToEdmInternal(null));
  }

  @Test
  void transform_invalidXmlRecord_expectFail() throws IOException, TransformationException {
    var recordContent = testUtils.readFileToBytes("record"+File.separator+"bad-order"+File.separator+"record-input.xml");

    ByteArrayInputStream xsltContent = new ByteArrayInputStream(
        Files.readAllBytes(Paths.get("src/test/resources/record/xsltFileContent.xsl")));

    when(service.getNewTransformerObject("identifier", xsltContent)).thenReturn(xsltTransformer);
    when(xsltTransformer.transformToBytes(recordContent, null))
        .thenThrow(new TransformationException(new Exception("Failing here")));

    RecordProcessingException exception = assertThrows(RecordProcessingException.class,
        () -> service.transform("identifier", xsltContent, recordContent));
    assertEquals("identifier", exception.getRecordId());
  }

  @Test
  void transformToEdmInternal_invalidXml_expectFail() throws IOException, TransformationException {
    var input = testUtils.readFileToBytes("record"+File.separator+"bad-order"+File.separator+"record-input.xml");

    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT).content(input)
        .recordId("1").build();

    when(objectProvider.getObject(anyString(), anyString(), anyString()))
        .thenReturn(xsltTransformer);
    when(xsltTransformer.transformToBytes(any(byte[].class), any(EuropeanaGeneratedIdsMap.class)))
        .thenThrow(new TransformationException(new Exception("Failing here")));

    RecordProcessingException exception = assertThrows(RecordProcessingException.class,
        () -> service.transformToEdmInternal(record));
    assertEquals("1", exception.getRecordId());
  }
}