package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
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
  public RecordInfo normalize(Record record) {
    requireNonNull(record, "Record must not be null");

    Normalizer normalizer;
    byte[] result;
    try {
      normalizer = normalizerFactory.getNormalizer();
      result = normalizer.normalize(record.getContentInputStream());
    } catch (NormalizationConfigurationException | NormalizationException e) {
      throw new RecordProcessingException(record.getProviderId(), e);
    }

    return new RecordInfo(Record.from(record, result));
  }
}
