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
  public RecordInfo normalize(Record recordToNormalize) {
    requireNonNull(recordToNormalize, "Record must not be null");

    final Normalizer normalizer;
    final byte[] result;
    try {
      normalizer = normalizerFactory.getNormalizer();
      result = normalizer.normalize(recordToNormalize.getContentInputStream());
    } catch (NormalizationConfigurationException | NormalizationException e) {
      throw new RecordProcessingException(recordToNormalize.getProviderId(), e);
    }

    return new RecordInfo(Record.from(recordToNormalize, result));
  }
}
