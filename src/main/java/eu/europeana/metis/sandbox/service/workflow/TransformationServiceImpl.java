package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.transformation.service.CacheValueSupplier.CacheValueSupplierException;
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
  public byte[] transform(String identifier, InputStream xsltContentInputStream, byte[] recordContent) {

    byte[] resultRecord;
    try {
      XsltTransformer transformer = getNewTransformerObject(identifier, xsltContentInputStream);
      resultRecord = transformer.transformToBytes(recordContent, null);
    } catch (TransformationException e) {
      throw new RecordProcessingException(identifier, e);
    }

    return resultRecord;
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

  protected XsltTransformer getNewTransformerObject(String identifier, InputStream xsltFile)
      throws TransformationException {
    return new XsltTransformer(identifier, xsltFile);
  }
}
