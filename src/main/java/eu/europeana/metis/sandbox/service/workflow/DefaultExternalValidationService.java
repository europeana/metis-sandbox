package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.NonRecoverableServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class DefaultExternalValidationService implements ExternalValidationService {

  private OrderingService orderingService;

  public DefaultExternalValidationService(
      OrderingService orderingService) {
    this.orderingService = orderingService;
  }

  @Override
  public Record validate(Record record) {

    var recordOrdered = orderingService.performOrdering(record);

    log.info("This is the input: {}", record.getContent());

    log.info("This is the output: {}", recordOrdered);

    return Record.from(record, recordOrdered, Step.VALIDATE_EXTERNAL);
  }


}
