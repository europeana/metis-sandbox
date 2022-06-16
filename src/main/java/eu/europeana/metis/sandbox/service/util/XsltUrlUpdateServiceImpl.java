package eu.europeana.metis.sandbox.service.util;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XsltUrlUpdateServiceImpl implements XsltUrlUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltUrlUpdateServiceImpl.class);
  private static final HttpClient httpClient = HttpClient.newBuilder().build();
  private final TransformXsltRepository transformXsltRepository;

  public XsltUrlUpdateServiceImpl(TransformXsltRepository transformXsltRepository) {
    this.transformXsltRepository = transformXsltRepository;
  }

  @Override
  public void updateXslt(String defaultXsltUrl) {

    InputStream xsltStream = null;
    HttpRequest httpRequest;
    try {
      httpRequest = HttpRequest.newBuilder()
                               .GET()
                               .uri(URI.create(defaultXsltUrl))
                               .build();

      xsltStream = httpClient.send(httpRequest, BodyHandlers.ofInputStream()).body();

    } catch (IOException | InterruptedException e) {
      // try to load resource as file
      xsltStream = getClass().getClassLoader().getResourceAsStream(defaultXsltUrl);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      LOGGER.warn("Error getting default transform XSLT ", e);
    } finally {
      if (xsltStream != null) {
        saveDefaultXslt(xsltStream);
        closeStream(xsltStream);
      }
    }
  }

  private void saveDefaultXslt(InputStream xsltStream) {
    try {
      final String transformXslt = IOUtils.toString(xsltStream, StandardCharsets.UTF_8);
      final var entity = transformXsltRepository.findById(1);

      if (entity.isPresent()) {
        if (!(transformXslt.equals(entity.get().getTransformXslt()))) {
          entity.get().setTransformXslt(transformXslt);
          transformXsltRepository.save(entity.get());
        }
      } else {
        transformXsltRepository.save(new TransformXsltEntity(transformXslt));
      }
    } catch (RuntimeException | IOException e) {
      LOGGER.warn("Error persisting default transform XSLT to Database", e);
    }
  }

  private void closeStream(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        LOGGER.error("Unable to close stream transform XSLT", e);
      }
    }
  }
}
