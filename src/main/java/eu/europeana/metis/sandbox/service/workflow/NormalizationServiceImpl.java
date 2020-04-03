package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.normalization.util.NormalizationConfigurationException;
import eu.europeana.normalization.util.NormalizationException;
import org.springframework.stereotype.Service;

@Service
class NormalizationServiceImpl implements NormalizationService {

  private final NormalizerFactory normalizerFactory;

  public NormalizationServiceImpl(NormalizerFactory normalizerFactory) {
    this.normalizerFactory = normalizerFactory;
  }

  @Override
  public Record normalize(Record record) {
    requireNonNull(record, "Record must not be null");

    Normalizer normalizer;
    NormalizationResult result;
    try {
      normalizer = normalizerFactory.getNormalizer();
      result = normalizer.normalize(record.getContent());
    } catch (NormalizationConfigurationException | NormalizationException e) {
      throw new RecordProcessingException(record.getRecordId(), e);
    }

    // TODO what to do with the NormalizationResult
    if (result.getErrorMessage() != null) {
      throw new RecordProcessingException(result.getErrorMessage(), record.getRecordId(), null);
    }

    return Record.from(record, result.getNormalizedRecordInEdmXml());
  }
}
