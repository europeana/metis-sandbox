package eu.europeana.metis.sandbox.service.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;

@ExtendWith({MockitoExtension.class})
@TestMethodOrder(OrderAnnotation.class)
class XsltUrlUpdateServiceTest {

  @Mock
  private TransformXsltRepository transformXsltRepository;

  @Mock
  private HttpClient httpClient;

  @Mock
  private LockRegistry lockRegistry;

  @InjectMocks
  private XsltUrlUpdateService xsltUrlUpdateService;

  @Test
  void updateXslt_ExpectSuccess() {
    //given
    when(lockRegistry.obtain(any())).thenReturn(mock(Lock.class));
    HttpResponse httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("<xslt></xslt>");
    CompletableFuture response = CompletableFuture.completedFuture(httpResponse);
    when(httpClient.sendAsync(any(HttpRequest.class), any())).thenReturn(response);

    // when
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    Mockito.verify(transformXsltRepository, times(1)).save(any());
  }

  @Test
  void updateXslt_Existent_ExpectSuccess() {
    // given
    when(lockRegistry.obtain(any())).thenReturn(mock(Lock.class));
    HttpResponse httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("<xslt></xslt>");
    CompletableFuture response = CompletableFuture.completedFuture(httpResponse);
    when(httpClient.sendAsync(any(HttpRequest.class), any())).thenReturn(response);

    // when
    when(transformXsltRepository.findFirstByIdIsNotNullOrderByIdAsc()).thenReturn(Optional.of(new TransformXsltEntity()));
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    //then
    Mockito.verify(transformXsltRepository, times(1)).save(any());
  }

  @Test
  void updateXslt_RepositorySave_RuntimeException_ExpectFail() {
    //given
    when(lockRegistry.obtain(any())).thenReturn(mock(Lock.class));
    HttpResponse httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("<xslt></xslt>");
    CompletableFuture response = CompletableFuture.completedFuture(httpResponse);
    when(httpClient.sendAsync(any(HttpRequest.class), any())).thenReturn(response);

    // when
    when(transformXsltRepository.save(any())).thenThrow(RuntimeException.class);

    // then
    assertDoesNotThrow(() -> xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt"));
  }

  @Test
  void updateXslt_RepositoryFind_RuntimeException_ExpectFail() {
    //given
    when(lockRegistry.obtain(any())).thenReturn(mock(Lock.class));
    HttpResponse httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn("<xslt></xslt>");
    CompletableFuture response = CompletableFuture.completedFuture(httpResponse);
    when(httpClient.sendAsync(any(HttpRequest.class), any())).thenReturn(response);

    // when
    when(transformXsltRepository.findFirstByIdIsNotNullOrderByIdAsc()).thenThrow(RuntimeException.class);
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    assertThrows(RuntimeException.class, () -> transformXsltRepository.findFirstByIdIsNotNullOrderByIdAsc());
    Mockito.verify(transformXsltRepository, never()).save(any());
  }

  @Test
  void updateXslt_Service_IllegalArgumentException_ExpectFail() {
    assertThrows(IllegalArgumentException.class, () -> xsltUrlUpdateService.updateXslt(""));
  }

  @Test
  void updateXslt_HttpClient_IllegalArgument_ExpectFail()  {
    // when
    when(httpClient.sendAsync(any(HttpRequest.class), any())).thenThrow(IllegalArgumentException.class);

    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    Mockito.verify(transformXsltRepository, never()).save(any());
  }

  @Test
  void updateXslt_HttpClient_NotFound_ExpectNoUpdate() {
    //given
    HttpResponse httpResponse = mock(HttpResponse.class);
    when(httpResponse.statusCode()).thenReturn(404);
    when(httpResponse.body()).thenReturn("<xslt></xslt>");
    CompletableFuture response = CompletableFuture.completedFuture(httpResponse);
    when(httpClient.sendAsync(any(HttpRequest.class), any())).thenReturn(response);

    // when
    xsltUrlUpdateService.updateXslt("http://document.domain:12345/xslt");
    // then
    Mockito.verify(transformXsltRepository, never()).save(any());
  }
}
