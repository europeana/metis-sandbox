package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.normalization.util.NormalizationConfigurationException;
import eu.europeana.normalization.util.NormalizationException;
import org.springframework.stereotype.Service;

@Service
public class NormalizeService {

  private final NormalizerFactory normalizerFactory = new NormalizerFactory();

  public String normalizeRecord(String recordData) throws NormalizationException {
    Normalizer normalizer;
    try {
      normalizer = normalizerFactory.getNormalizer();
    } catch (NormalizationConfigurationException e) {
      throw new NormalizationException("Normalization configuration failed", e);
    }
    NormalizationResult normalizationResult = normalizer.normalize(recordData);
    return normalizationResult.getNormalizedRecordInEdmXml();
  }
}

