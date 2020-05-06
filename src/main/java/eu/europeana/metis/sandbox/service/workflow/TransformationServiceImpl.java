package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
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
  public RecordInfo transform(Record record) {
    requireNonNull(record, "Record must not be null");

    String recordTransformed;
    try {
      var europeanaGeneratedIdsMap = new EuropeanaIdCreator()
          .constructEuropeanaId(record.getContentString(), record.getDatasetId().toString());
      var transformer = getTransformer(record.getDatasetName(),
          record.getCountry().xmlValue(), record.getLanguage().xmlValue());
      recordTransformed = transformer.transform(record.getContent(), europeanaGeneratedIdsMap)
          .toString();
    } catch (TransformationException | EuropeanaIdException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    return new RecordInfo(Record.from(record, recordTransformed));
  }

  private XsltTransformer getTransformer(String datasetName, String edmCountry,
      String edmLanguage) {
    return xsltTransformer.getObject(datasetName, edmCountry, edmLanguage);
  }
}
