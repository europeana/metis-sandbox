package eu.europeana.metis.sandbox.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.service.util.XsltUrlUpdateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XsltUrlUpdateSchedulerTest {

  @Mock
  private XsltUrlUpdateServiceImpl xsltUrlUpdateService;

  @InjectMocks
  private XsltUrlUpdateScheduler xsltUrlUpdateScheduler;

  @Test
  void updateDefaultXsltUrl_expectSuccess() {

    xsltUrlUpdateScheduler.updateDefaultXsltUrl();
    // verify that the scheduler called the service
    verify(xsltUrlUpdateService).updateXslt(any());
  }

}
