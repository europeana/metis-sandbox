package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import org.springframework.stereotype.Service;

@Service
class TransformationServiceImpl implements TransformationService {

  private final TransformXsltRepository transformXsltRepository;

  public TransformationServiceImpl(TransformXsltRepository transformXsltRepository) {
    this.transformXsltRepository = transformXsltRepository;
  }

  @Override
  public RecordInfo transform(Record record) {
    requireNonNull(record, "Record must not be null");

    byte[] recordTransformed;
    try {
      var europeanaGeneratedIdsMap = new EuropeanaIdCreator()
          .constructEuropeanaId(record.getContentInputStream(), record.getDatasetId());
      var transformer = getTransformer(getXmlDatasetName(record),
          record.getCountry().xmlValue(), record.getLanguage().name().toLowerCase());
      recordTransformed = transformer
          .transformToBytes(record.getContent(), europeanaGeneratedIdsMap);
    } catch (TransformationException | EuropeanaIdException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    return new RecordInfo(Record.from(record, recordTransformed));
  }

  private XsltTransformer getTransformer(String datasetName, String edmCountry,
      String edmLanguage) throws TransformationException {
    var xsltTransformEntity = transformXsltRepository.findById(1);
    String  xsltTransform = null;
    if (xsltTransformEntity.isPresent()){
      xsltTransform = xsltTransformEntity.get().getTransformXslt();
    }
    return new XsltTransformer(xsltTransform, datasetName, edmCountry, edmLanguage);
  }

  private String getXmlDatasetName(Record record) {
    return String.join("_", record.getDatasetId(), record.getDatasetName());
  }
}
