package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XsltUrlUpdateServiceImpl implements XsltUrlUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltUrlUpdateServiceImpl.class);
  private static final HttpClient httpClient = HttpClient.newBuilder().build();
  private final TransformXsltRepository transformXsltRepository;

  public XsltUrlUpdateServiceImpl(TransformXsltRepository transformXsltRepository) {
    this.transformXsltRepository = transformXsltRepository;
  }

  @Override
  @Transactional
  public void updateXslt(String defaultXsltUrl) {

    HttpRequest httpRequest = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(defaultXsltUrl))
        .build();

    try (final InputStream xsltStream = httpClient.send(httpRequest, BodyHandlers.ofInputStream())
        .body()) {
      String transformXslt = new String(xsltStream.readAllBytes(),
          StandardCharsets.UTF_8);
      var entity = transformXsltRepository.findByTransformXslt(transformXslt);

      if (entity != null && entity.isPresent()) {
        transformXsltRepository.save(entity.get());
      }
      else {
        transformXsltRepository.save(new TransformXsltEntity(transformXslt));
      }
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Error updating default transform XSLT ", e);
    }

  }
}
