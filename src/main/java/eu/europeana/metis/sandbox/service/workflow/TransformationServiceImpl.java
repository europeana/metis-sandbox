package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import java.io.InputStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
class TransformationServiceImpl implements TransformationService {

  private final ObjectProvider<XsltTransformer> xsltTransformer;

  public TransformationServiceImpl(
      ObjectProvider<XsltTransformer> xsltTransformer) {
    this.xsltTransformer = xsltTransformer;
  }

  @Override
  public RecordInfo transformToEdmInternal(Record record) {
    requireNonNull(record, "Record must not be null");

    byte[] recordTransformed;
    try {
      EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = new EuropeanaIdCreator()
          .constructEuropeanaId(record.getContentInputStream(), record.getDatasetId());
      XsltTransformer transformer = getTransformer(getJoinDatasetIdDatasetName(record),
          record.getCountry().xmlValue(), record.getLanguage().name().toLowerCase());
      recordTransformed = transformer
          .transformToBytes(record.getContent(), europeanaGeneratedIdsMap);
    } catch (TransformationException | EuropeanaIdException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    return new RecordInfo(Record.from(record, recordTransformed));
  }

  @Override
  public byte[] transform(String datasetId, String datasetName, InputStream xsltToEdmExternal,
      Country country, Language language, byte[] recordContent) {

    byte[] recordToEdmExternal;
    try {
      XsltTransformer transformer = new XsltTransformer(
          getJoinDatasetIdDatasetName(datasetId, datasetName), xsltToEdmExternal,
          getJoinDatasetIdDatasetName(datasetId, datasetName), country.xmlValue(), language.name().toLowerCase());
      recordToEdmExternal = transformer.transformToBytes(recordContent, null);
    } catch (TransformationException e) {
      throw new RecordProcessingException(datasetId, e);
    }

    return recordToEdmExternal;
  }

  private XsltTransformer getTransformer(String datasetName, String edmCountry,
      String edmLanguage) {
    return xsltTransformer.getObject(datasetName, edmCountry, edmLanguage);
  }

  private String getJoinDatasetIdDatasetName(Record record) {
    return getJoinDatasetIdDatasetName(record.getDatasetId(), record.getDatasetName());
  }

  private String getJoinDatasetIdDatasetName(String datasetId, String datasetName){
    return String.join("_", datasetId, datasetName);
  }
}
