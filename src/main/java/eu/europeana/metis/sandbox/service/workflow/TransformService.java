package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.batch.common.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class TransformService {

  private final TransformXsltRepository transformXsltRepository;

  public TransformService(TransformXsltRepository transformXsltRepository) {
    this.transformXsltRepository = transformXsltRepository;
  }

  public String transformRecord(String recordId,
      String recordData,
      String xsltId,
      TransformationBatchJobSubType subType,
      String datasetId,
      String datasetName,
      String datasetCountry,
      String datasetLanguage) {

    String xsltContent = transformXsltRepository.findById(Integer.valueOf(xsltId))
                                                .map(TransformXsltEntity::getTransformXslt).orElseThrow();
    byte[] contentBytes = recordData.getBytes(StandardCharsets.UTF_8);
    InputStream xsltInputStream = new ByteArrayInputStream(xsltContent.getBytes(StandardCharsets.UTF_8));

    try {
      return switch (subType) {
        case EXTERNAL -> transformExternal(recordId, contentBytes, xsltInputStream);
        case INTERNAL -> transformInternal(recordId, contentBytes, xsltInputStream, datasetId, datasetName, datasetCountry, datasetLanguage);
      };
    } catch (Exception e) {
      throw new RuntimeException("Transformation failed", e);
    }
  }

  private String transformExternal(String recordId, byte[] contentBytes, InputStream xsltInputStream) throws Exception {
    try (XsltTransformer xsltTransformer = new XsltTransformer(recordId, xsltInputStream);
        StringWriter writer = xsltTransformer.transform(contentBytes, null)) {
      return writer.toString();
    }
  }

  private String transformInternal(String recordId, byte[] contentBytes, InputStream xsltInputStream,
      String datasetId, String datasetName, String datasetCountry, String datasetLanguage) throws Exception {
    final String datasetIdDatasetName = getJoinDatasetIdDatasetName(datasetId, datasetName);
    final EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = prepareEuropeanaGeneratedIdsMap(contentBytes, datasetId);

    try (XsltTransformer xsltTransformer = new XsltTransformer("xsltKey", xsltInputStream, datasetIdDatasetName, datasetCountry, datasetLanguage);
        StringWriter writer = xsltTransformer.transform(contentBytes, europeanaGeneratedIdsMap)) {
      return writer.toString();
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

