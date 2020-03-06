package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.NonRecoverableServiceException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectFactory;
import org.xml.sax.helpers.DefaultHandler;

@ExtendWith(MockitoExtension.class)
class DefaultXmlRecordProcessorServiceTest {

  private final TestUtils utils = new TestUtils();

  @Mock
  private ObjectFactory<SAXParserFactory> objectFactory;

  @Mock
  private SAXParser saxParser;

  @Spy
  private SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

  @InjectMocks
  private DefaultXmlRecordProcessorService service;

  @Test
  void getRecordId_expectSuccess() throws Exception {
    var xmlRecord = utils.readFileToString("record/valid-record.xml");

    when(objectFactory.getObject()).thenReturn(saxParserFactory);

    String recordId = service.getRecordId(xmlRecord);

    assertEquals("URN:NBN:SI:doc-35SZSOCF", recordId);
  }

  @Test
  void getRecordId_nullRecord_expectFail() {
    assertThrows(NullPointerException.class, () -> service.getRecordId(null));
  }

  @Test
  void getRecordId_nonRecoverableException_expectFail() throws Exception {
    var xmlRecord = utils.readFileToString("record/valid-record.xml");

    when(objectFactory.getObject()).thenReturn(saxParserFactory);
    when(saxParserFactory.newSAXParser()).thenThrow(new ParserConfigurationException());

    assertThrows(NonRecoverableServiceException.class, () -> service.getRecordId(xmlRecord));
  }

  @Test
  void getRecordId_serviceException_expectFail() throws Exception {
    var xmlRecord = utils.readFileToString("record/valid-record.xml");

    when(objectFactory.getObject()).thenReturn(saxParserFactory);
    when(saxParserFactory.newSAXParser()).thenReturn(saxParser);
    doThrow(new IOException()).when(saxParser).parse(any(InputStream.class), any(DefaultHandler.class));

    assertThrows(ServiceException.class, () -> service.getRecordId(xmlRecord));
  }

  @Test
  void getRecordId_recordMissingId_expectFail() throws Exception {
    var xmlRecord = utils.readFileToString("record/record-missing-id.xml");

    when(objectFactory.getObject()).thenReturn(saxParserFactory);

    assertThrows(IllegalArgumentException.class, () -> service.getRecordId(xmlRecord));
  }
}