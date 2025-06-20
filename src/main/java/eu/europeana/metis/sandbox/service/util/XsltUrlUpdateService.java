package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

/**
 * Service for updating and storing default XSLT transformations from a given URL.
 */
@Service
public class XsltUrlUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TransformXsltRepository transformXsltRepository;

  private final LockRegistry lockRegistry;

  private final HttpClient httpClient;

  /**
   * Constructor.
   *
   * @param transformXsltRepository repository for managing and accessing TransformXSLT entities
   * @param lockRegistry registry for getting locks to control shared resource access
   * @param httpClient HTTP client for sending requests and handling responses
   */
  public XsltUrlUpdateService(
      TransformXsltRepository transformXsltRepository, LockRegistry lockRegistry, HttpClient httpClient) {
    this.transformXsltRepository = transformXsltRepository;
    this.lockRegistry = lockRegistry;
    this.httpClient = httpClient;
  }

  /**
   * Updates the default XSLT transformation by fetching the content from the provided URL.
   *
   * @param defaultXsltUrl url from which the XSLT transformation will be fetched
   */
  public void updateXslt(String defaultXsltUrl) {

    final HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(defaultXsltUrl)).build();
    try {
      HttpResponse<String> response = httpClient.sendAsync(request, BodyHandlers.ofString()).get();
      if (response.statusCode() == HttpStatus.OK.value()) {
        // TODO: should the response body (xslt) be validated?
        saveDefaultXslt(response.body());
      } else {
        LOGGER.warn("Failed to update default transform XSLT from URL: {} \nResponse status code: {}", defaultXsltUrl,
            response.body());
      }
    } catch (RuntimeException | InterruptedException | ExecutionException e) {
      LOGGER.error("Failed to update default transform XSLT from URL: {} \n{}", defaultXsltUrl, e);
      Thread.currentThread().interrupt();
    }
  }

  private void saveDefaultXslt(String newTransformXslt) {
    final Lock lock = lockRegistry.obtain("saveDefaultXslt");
    try {
      lock.lock();
      LOGGER.info("Save default xslt lock, Locked");
      final Optional<TransformXsltEntity> entity = transformXsltRepository.findFirstByTypeOrderById(XsltType.DEFAULT);

      if (entity.isPresent()) {
        if (!(newTransformXslt.equals(entity.get().getTransformXslt()))) {
          entity.get().setTransformXslt(newTransformXslt);
          transformXsltRepository.save(entity.get());
        }
      } else {
        TransformXsltEntity transformXsltEntity = new TransformXsltEntity(XsltType.DEFAULT, newTransformXslt);
        transformXsltRepository.save(transformXsltEntity);
      }
    } catch (RuntimeException e) {
      LOGGER.error("Failed to persist default transform XSLT from URL: {} \n{}", newTransformXslt, e);
    } finally {
      LOGGER.info("Save default xslt lock, Unlocked");
      lock.unlock();
    }
  }
}
