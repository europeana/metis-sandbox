package eu.europeana.metis.sandbox.service.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
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
    //given
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("<xslt></xslt>")));
    // when
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    Mockito.verify(transformXsltRepository, times(1)).save(any());
  }

  @Test
  @Order(2)
  void updateXslt_Existent_ExpectSuccess() {
    // given
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("<xslt></xslt>")));
    // when
    when(transformXsltRepository.findFirstByIdIsNotNullOrderByIdAsc()).thenReturn(Optional.of(new TransformXsltEntity()));
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    //then
    Mockito.verify(transformXsltRepository, times(1)).save(any());
  }

  @Test
  @Order(3)
  void updateXslt_RepositorySave_RuntimeException_ExpectFail() {
    //given
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("<xslt></xslt>")));
    // when
    when(transformXsltRepository.save(any())).thenThrow(RuntimeException.class);
    // then
    assertThrows(RuntimeException.class, () -> transformXsltRepository.save(any()));
    assertDoesNotThrow(() -> xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt"));
  }

  @Test
  @Order(4)
  void updateXslt_RepositoryFind_RuntimeException_ExpectFail() {
    //given
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("<xslt></xslt>")));
    // when
    when(transformXsltRepository.findFirstByIdIsNotNullOrderByIdAsc()).thenThrow(RuntimeException.class);
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    assertThrows(RuntimeException.class, () -> transformXsltRepository.findFirstByIdIsNotNullOrderByIdAsc());
    Mockito.verify(transformXsltRepository, never()).save(any());
  }

  @Test
  @Order(5)
  void updateXslt_Service_IllegalArgumentException_ExpectFail() {

    assertThrows(IllegalArgumentException.class, () -> xsltUrlUpdateService.updateXslt(""));
  }

  @Test
  @Order(6)
  void updateXslt_HttpClient_IllegalArgument_ExpectFail() throws ReflectiveOperationException {
    //given
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(ok("<xslt></xslt>")));
    // when
    when(httpClient.sendAsync(any(HttpRequest.class), any())).thenThrow(IllegalArgumentException.class);
    setFinalStaticField(XsltUrlUpdateServiceImpl.class, "httpClient", httpClient);
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    Mockito.verify(transformXsltRepository, never()).save(any());
  }

  @Test
  @Order(7)
  void updateXslt_HttpClient_NotFound_ExpectNoUpdate() {
    //given
    wm.stubFor(get("/xslt")
        .withHost(equalTo("document.domain"))
        .withPort(12345)
        .willReturn(notFound()));
    // when
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    Mockito.verify(transformXsltRepository, never()).save(any());
  }


  private void setFinalStaticField(Class<?> clazz, String fieldName, Object value) throws ReflectiveOperationException {

    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);

    Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(fieldName, value);
  }
}