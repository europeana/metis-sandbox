package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.batch.common.TransformationBatchJobSubType;
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
   * Transforms a record using an XSLT transformation based on the specified subtype.
   *
   * <p>Uses either external or internal transformation logic depending on the provided subtype.
   * <p>The XSLT content is retrieved using the given XSLT ID.
   *
   * @param recordId the unique identifier of the record to be transformed
   * @param recordData the data content of the record to be transformed
   * @param xsltId the identifier of the XSLT to apply during the transformation
   * @param subType defines whether the transformation is EXTERNAL or INTERNAL
   * @param datasetId the identifier of the dataset associated with the record
   * @param datasetName the name of the dataset associated with the record
   * @param datasetCountry the country associated with the dataset
   * @param datasetLanguage the language associated with the dataset
   * @return the transformed record content as a string
   * @throws TransformationException if an error occurs during the transformation process
   */
  public String transformRecord(String recordId,
      String recordData,
      String xsltId,
      TransformationBatchJobSubType subType,
      String datasetId,
      String datasetName,
      String datasetCountry,
      String datasetLanguage) throws TransformationException {

    String xsltContent = transformXsltRepository.findById(Integer.valueOf(xsltId))
                                                .map(TransformXsltEntity::getTransformXslt).orElseThrow();
    byte[] contentBytes = recordData.getBytes(StandardCharsets.UTF_8);
    InputStream xsltInputStream = new ByteArrayInputStream(xsltContent.getBytes(StandardCharsets.UTF_8));

    return switch (subType) {
      case EXTERNAL -> transformExternal(recordId, contentBytes, xsltInputStream);
      case INTERNAL -> transformInternal(contentBytes, xsltInputStream, datasetId, datasetName, datasetCountry, datasetLanguage);
    };
  }

  private String transformExternal(String recordId, byte[] contentBytes, InputStream xsltInputStream)
      throws TransformationException {
    try (XsltTransformer xsltTransformer = new XsltTransformer(recordId, xsltInputStream);
        StringWriter writer = xsltTransformer.transform(contentBytes, null)) {
      return writer.toString();
    } catch (IOException e) {
      throw new TransformationException(e);
    }
  }

  private String transformInternal(byte[] contentBytes, InputStream xsltInputStream,
      String datasetId, String datasetName, String datasetCountry, String datasetLanguage) throws TransformationException {
    final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap;
    try {
      europeanaGeneratedIdsMap = prepareEuropeanaGeneratedIdsMap(contentBytes, datasetId);
    } catch (EuropeanaIdException e) {
      throw new TransformationException(e);
    }

    final String datasetIdDatasetName = getJoinDatasetIdDatasetName(datasetId, datasetName);
    try (XsltTransformer xsltTransformer = new XsltTransformer("xsltKey", xsltInputStream, datasetIdDatasetName, datasetCountry,
        datasetLanguage);
        StringWriter writer = xsltTransformer.transform(contentBytes, europeanaGeneratedIdsMap)) {
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
}

