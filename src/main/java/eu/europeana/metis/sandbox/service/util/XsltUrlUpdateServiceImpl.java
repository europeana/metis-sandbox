package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

@Service
public class XsltUrlUpdateServiceImpl implements XsltUrlUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltUrlUpdateServiceImpl.class);

  private final TransformXsltRepository transformXsltRepository;

  private final LockRegistry lockRegistry;

  private final HttpClient httpClient;

  public XsltUrlUpdateServiceImpl(TransformXsltRepository transformXsltRepository,
      LockRegistry lockRegistry,
      HttpClient httpClient) {
    this.transformXsltRepository = transformXsltRepository;
    this.lockRegistry = lockRegistry;
    this.httpClient = httpClient;
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
    final Lock lock = lockRegistry.obtain(TransformXsltRepository.LOCK_NAME_KEY);
    try {
      lock.lock();
      LOGGER.info("Save default xslt lock, Locked");
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
    } finally {
      LOGGER.info("Save default xslt lock, Unlocked");
      lock.unlock();
    }
  }
}
