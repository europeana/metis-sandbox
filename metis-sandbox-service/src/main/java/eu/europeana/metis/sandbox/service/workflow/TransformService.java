package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Service responsible for transforming records using XSLT transformations.
 */
@AllArgsConstructor
@Service
public class TransformService {

  private final TransformXsltRepository transformXsltRepository;

  /**
   * Transforms the input record using the provided XSLT.
   *
   * @param recordBytes the byte array representing the record to transform
   * @param xsltBytes the byte array containing the XSLT transformation definition
   * @param xsltCacheKey a unique key used to cache the XSLT transformation instance
   * @return the transformed record as a string
   * @throws TransformationException if an error occurs during the transformation process
   */
  public String transformExternal(byte[] recordBytes, byte[] xsltBytes, String xsltCacheKey)
      throws TransformationException {
    try (InputStream xsltInputStream = new ByteArrayInputStream(xsltBytes);
        XsltTransformer xsltTransformer = new XsltTransformer(xsltCacheKey, xsltInputStream);
        StringWriter writer = xsltTransformer.transform(recordBytes, null)) {
      return writer.toString();
    } catch (IOException e) {
      throw new TransformationException(e);
    }
  }

  /**
   * Transforms the input record using the provided XSLT and dataset context.
   *
   * @param recordBytes the byte array representing the record to transform
   * @param xsltBytes the byte array containing the XSLT transformation definition
   * @param xsltCacheKey a unique key used to cache the XSLT transformation instance
   * @param transformDatasetContext an object containing contextual information about the dataset, including its ID, name,
   * country, and language
   * @return the transformed record as a string
   * @throws TransformationException if an error occurs during the transformation process, such as preparing the IDs map or during
   * the XSLT transformation
   */
  public String transformInternal(byte[] recordBytes, byte[] xsltBytes, String xsltCacheKey,
      TransformDatasetContext transformDatasetContext) throws TransformationException {
    final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap;
    try {
      europeanaGeneratedIdsMap = prepareEuropeanaGeneratedIdsMap(recordBytes, transformDatasetContext.datasetId());
    } catch (EuropeanaIdException e) {
      throw new TransformationException(e);
    }

    final String datasetIdDatasetName = getJoinDatasetIdDatasetName(transformDatasetContext.datasetId(),
        transformDatasetContext.datasetName());
    try (InputStream xsltInputStream = new ByteArrayInputStream(xsltBytes);
        XsltTransformer xsltTransformer = new XsltTransformer(
            xsltCacheKey, xsltInputStream, datasetIdDatasetName, transformDatasetContext.datasetCountry(),
            transformDatasetContext.datasetLanguage());
        StringWriter writer = xsltTransformer.transform(recordBytes, europeanaGeneratedIdsMap)) {
      return writer.toString();
    } catch (IOException e) {
      throw new TransformationException(e);
    }
  }

  private EuropeanaGeneratedIdsMap prepareEuropeanaGeneratedIdsMap(byte[] content, String datasetId)
      throws EuropeanaIdException {
    if (StringUtils.isBlank(datasetId)) {
      return null;
    }
    String fileDataString = new String(content, StandardCharsets.UTF_8);
    EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
    return europeanIdCreator.constructEuropeanaId(fileDataString, datasetId);
  }

  private String getJoinDatasetIdDatasetName(String datasetId, String datasetName) {
    return String.join("_", datasetId, datasetName);
  }

  /**
   * Retrieves the XSLT content as a byte array for the given XSLT ID.
   *
   * @param xsltId the unique identifier of the XSLT transformation to retrieve
   * @return the XSLT content as a byte array encoded in UTF-8
   * @throws NoSuchElementException if the XSLT transformation is not found for the provided ID
   */
  public byte[] getXsltBytes(Integer xsltId) {
    return transformXsltRepository.findById(xsltId)
                                  .map(TransformXsltEntity::getTransformXslt)
                                  .map(s -> s.getBytes(StandardCharsets.UTF_8))
                                  .orElseThrow(() -> new NoSuchElementException("No XSLT found for id " + xsltId));
  }

  /**
   * A record representing the context of a dataset used in the transformation process.
   * <p>
   * It is primarily used within transformation workflows to provide metadata about the dataset that could influence the
   * transformation output.
   */
  public record TransformDatasetContext(
      String datasetId,
      String datasetName,
      String datasetCountry,
      String datasetLanguage
  ) {

  }
}

