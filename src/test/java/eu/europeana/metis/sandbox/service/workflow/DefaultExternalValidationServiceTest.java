package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.transformation.service.XsltTransformer;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectFactory;

class DefaultExternalValidationServiceTest {

  @Mock
  private ObjectFactory<XsltTransformer> orderObjectFactory;

  @Mock
  private XsltTransformer xsltSorter;

  @InjectMocks
  private DefaultExternalValidationService service;

  @Test
  void validate_expectSuccess() {
    Record record = Record.builder().

    //service.validate(null);

  }
}