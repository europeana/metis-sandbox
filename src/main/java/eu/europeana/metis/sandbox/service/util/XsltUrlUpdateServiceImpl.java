package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class XsltUrlUpdateServiceImpl implements XsltUrlUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltUrlUpdateServiceImpl.class);

  private final TransformXsltRepository transformXsltRepository;

  private static final HttpClient httpClient = HttpClient.newBuilder().version(Version.HTTP_2)
                                                         .followRedirects(Redirect.NORMAL)
                                                         .connectTimeout(Duration.ofSeconds(5))
                                                         .build();

  public XsltUrlUpdateServiceImpl(TransformXsltRepository transformXsltRepository) {
    this.transformXsltRepository = transformXsltRepository;
  }

  @Override
  public void updateXslt(String defaultXsltUrl) {

    final HttpRequest request = HttpRequest.newBuilder()
                                           .GET()
                                           .uri(URI.create(defaultXsltUrl))
                                           .build();
    try {
      CompletableFuture<HttpResponse<String>> response =
          httpClient.sendAsync(request, BodyHandlers.ofString());

      String responseBody = response.thenApply(HttpResponse::body).get();
      int responseStatusCode = response.thenApply(HttpResponse::statusCode).get();
      if (responseStatusCode == 200) {
        // TODO: should the response body (xslt) be validated?
        saveDefaultXslt(responseBody);
      } else {
        LOGGER.warn("Failed to update default transform XSLT from URL: {} \nResponse status code: {}", defaultXsltUrl,
            responseStatusCode);
      }
    } catch (RuntimeException | InterruptedException | ExecutionException e) {
      LOGGER.error("Failed to update default transform XSLT from URL: {} \n{}", defaultXsltUrl, e);
      Thread.currentThread().interrupt();
    }
  }

  private void saveDefaultXslt(String newTransformXslt) {
    try {
      final Optional<TransformXsltEntity> entity = transformXsltRepository.findFirstByIdIsNotNullOrderByIdAsc();

      if (entity.isPresent()) {
        if (!(newTransformXslt.equals(entity.get().getTransformXslt()))) {
          entity.get().setTransformXslt(newTransformXslt);
          transformXsltRepository.save(entity.get());
        }
      } else {
        transformXsltRepository.save(new TransformXsltEntity(newTransformXslt));
      }
    } catch (RuntimeException e) {
      LOGGER.error("Failed to persist default transform XSLT from URL: {} \n{}", newTransformXslt, e);
    }
  }
}
