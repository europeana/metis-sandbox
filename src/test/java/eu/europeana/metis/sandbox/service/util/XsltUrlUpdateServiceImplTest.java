package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XsltUrlUpdateServiceImplTest {

  @Mock
  private TransformXsltRepository transformXsltRepository;

  @InjectMocks
  private XsltUrlUpdateServiceImpl xsltUrlUpdateService;

  @Test
  void updateXslt_ExpectError() {
    final LogCaptor logCaptor = LogCaptor.forClass(XsltUrlUpdateServiceImpl.class);
    xsltUrlUpdateService.updateXslt("");
    assertLogCaptor(logCaptor);
  }

  private void assertLogCaptor(LogCaptor logCaptor) {
    assertEquals(1, logCaptor.getWarnLogs().size());
    final String testMessage = logCaptor.getWarnLogs().stream().findFirst().get();
    assertTrue(testMessage.contains("Error getting default transform XSLT"));
  }
}