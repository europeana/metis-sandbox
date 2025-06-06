package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.normalization.util.NormalizationConfigurationException;
import eu.europeana.normalization.util.NormalizationException;
import org.springframework.stereotype.Service;

@Service
public class NormalizeService {

  private final NormalizerFactory normalizerFactory = new NormalizerFactory();

  public String normalizeRecord(String recordData) throws NormalizationConfigurationException, NormalizationException {
    NormalizationResult normalizationResult = normalizerFactory.getNormalizer().normalize(recordData);
    return normalizationResult.getNormalizedRecordInEdmXml();
  }
}

