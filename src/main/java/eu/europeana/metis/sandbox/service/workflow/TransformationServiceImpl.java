package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
class TransformationServiceImpl implements TransformationService {

  private final DatasetRepository datasetRepository;
  private final ObjectProvider<XsltTransformer> xsltTransformer;

  public TransformationServiceImpl(
      DatasetRepository datasetRepository,
      ObjectProvider<XsltTransformer> xsltTransformer) {
    this.datasetRepository = datasetRepository;
    this.xsltTransformer = xsltTransformer;
  }

  @Override
  public RecordInfo transform(Record record) {
    requireNonNull(record, "Record must not be null");

    byte[] recordTransformed;
    try {
      EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = new EuropeanaIdCreator()
          .constructEuropeanaId(record.getContentInputStream(), record.getDatasetId());
      XsltTransformer transformer = getTransformer(getXmlDatasetName(record),
          record.getCountry().xmlValue(), record.getLanguage().name().toLowerCase());
      recordTransformed = transformer
          .transformToBytes(record.getContent(), europeanaGeneratedIdsMap);
    } catch (TransformationException | EuropeanaIdException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    return new RecordInfo(Record.from(record, recordTransformed));
  }

  @Override
  public RecordInfo transformToEdmExternal(Record record) {
    requireNonNull(record, "Record must not be null");

    byte[] recordToEdmExternal;
    try {
      EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = new EuropeanaIdCreator()
          .constructEuropeanaId(record.getContentInputStream(), record.getDatasetId());
      String xsltToEdmExternal = datasetRepository.getOne(Integer.valueOf(record.getDatasetId())).getXsltTransformerEdmExternal();
      XsltTransformer transformer = new XsltTransformer(xsltToEdmExternal, getXmlDatasetName(record),
          record.getCountry().xmlValue(), record.getLanguage().name().toLowerCase());
      recordToEdmExternal = transformer
          .transformToBytes(record.getContent(), europeanaGeneratedIdsMap);
    } catch (TransformationException | EuropeanaIdException e){
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    return new RecordInfo(Record.from(record, recordToEdmExternal));
  }

  private XsltTransformer getTransformer(String datasetName, String edmCountry,
      String edmLanguage) {
    return xsltTransformer.getObject(datasetName, edmCountry, edmLanguage);
  }

  private String getXmlDatasetName(Record record) {
    return String.join("_", record.getDatasetId(), record.getDatasetName());
  }
}
