package eu.europeana.metis.sandbox.service.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Optional;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
@TestMethodOrder(OrderAnnotation.class)
class XsltUrlUpdateServiceImplTest {

  @Mock
  private TransformXsltRepository transformXsltRepository;

  @Mock
  private HttpClient httpClient;

  @InjectMocks
  private XsltUrlUpdateServiceImpl xsltUrlUpdateService;

  @RegisterExtension
  static WireMockExtension wm = WireMockExtension.newInstance()
                                                 .options(wireMockConfig().port(12345))
                                                 .proxyMode(true)
                                                 .build();

  @Test
  @Order(1)
  void updateXslt_ExpectSuccess() {
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("1")));

    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xlst");

    Mockito.verify(transformXsltRepository, times(1)).save(any());
  }

  @Test
  @Order(2)
  void updateXslt_existent_ExpectSuccess() {
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("1")));
    when(transformXsltRepository.findById(anyInt())).thenReturn(Optional.of(new TransformXsltEntity()));

    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xlst");

    Mockito.verify(transformXsltRepository, times(1)).save(any());
  }

  @Test
  @Order(3)
  void updateXslt_ExpectFail() {
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))

        .withPort(12345)
        .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

    doThrow(RuntimeException.class).when(transformXsltRepository).save(any());
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xlst");
  }

  @Test
  @Order(4)
  void updateXslt_ExpectError() {
    xsltUrlUpdateService.updateXslt("");
  }

  @Test
  @Order(5)
  void updateXslt_LoadAsFile_ExpectFail() throws IOException, ReflectiveOperationException, InterruptedException {
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("1")));
    doThrow(IOException.class).when(httpClient).send(any(HttpRequest.class), any());

    setFinalStaticField(XsltUrlUpdateServiceImpl.class, "httpClient", httpClient);

    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xlst");

    Mockito.verify(transformXsltRepository, never()).save(any());
  }

  private static void setFinalStaticField(Class<?> clazz, String fieldName, Object value)
      throws ReflectiveOperationException {

    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);

    Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, value);
  }
}