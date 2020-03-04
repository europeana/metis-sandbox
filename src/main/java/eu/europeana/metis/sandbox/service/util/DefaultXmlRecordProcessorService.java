package eu.europeana.metis.sandbox.service.util;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.NonRecoverableServiceException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Service
class DefaultXmlRecordProcessorService implements XmlRecordProcessorService {

  private final ObjectFactory<SAXParserFactory> objectFactory;

  public DefaultXmlRecordProcessorService(ObjectFactory<SAXParserFactory> objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public String getRecordId(String record) {
    requireNonNull(record, "Record must not be null");

    SAXParser parser;
    SAXParserFactory factory = getSAXParserFactory();
    var recordHandler = new RecordHandler();
    try {
      parser = factory.newSAXParser();
      var stream = new ByteArrayInputStream(record.getBytes());
      parser.parse(stream, recordHandler);
    } catch (ParserConfigurationException pce) {
      throw new NonRecoverableServiceException(
          "Error while parsing a xml record: " + pce.getMessage(), pce);
    } catch (SAXException | IOException e) {
      throw new ServiceException("Error while parsing a xml record: " + e.getMessage(), e);
    }

    if (recordHandler.getRecordId() == null) {
      throw new IllegalArgumentException("Provided xml record does not have a valid id");
    }

    return recordHandler.getRecordId();
  }

  private SAXParserFactory getSAXParserFactory() {
    return objectFactory.getObject();
  }

  private static class RecordHandler extends DefaultHandler {

    private static final String ID_ELEMENT = "edm:ProvidedCHO";
    private static final String ID_ATTRIBUTE = "rdf:about";

    private String recordId;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      if (qName.equalsIgnoreCase(ID_ELEMENT)) {
        recordId = attributes.getValue(ID_ATTRIBUTE);
      }
    }

    public String getRecordId() {
      return recordId;
    }
  }
}
