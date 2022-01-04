package eu.europeana.metis.sandbox.service.record;

import eu.europeana.corelib.solr.bean.impl.FullBeanImpl;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.fullbean.RdfToFullBeanConverter;
import eu.europeana.indexing.solr.EdmLabel;
import eu.europeana.indexing.tiers.ClassifierFactory;
import eu.europeana.indexing.tiers.view.FakeTierCalculationProvider;
import eu.europeana.indexing.tiers.view.RecordTierCalculationSummary;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.indexing.utils.RdfTierUtils;
import eu.europeana.indexing.utils.RdfWrapper;
import eu.europeana.indexing.utils.SolrTier;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordParsingException;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.schema.convert.RdfConversionUtils;
import eu.europeana.metis.schema.convert.SerializationException;
import eu.europeana.metis.schema.jibx.RDF;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Service for calculating record tier statistics
 */
@Service
public class RecordTierCalculationServiceImpl implements RecordTierCaclulationService {

  private final RecordLogRepository recordLogRepository;

  public RecordTierCalculationServiceImpl(RecordLogRepository recordLogRepository) {
    this.recordLogRepository = recordLogRepository;
  }

  @Override
  public RecordTierCalculationView calculateTiers(String recordId, String datasetId) {
    //Retrieve record from the database
    final RecordLogEntity recordLog = recordLogRepository.findRecordLog(recordId, datasetId, Step.MEDIA_PROCESS);

    RecordTierCalculationView recordTierCalculationView = null;
    if (Objects.nonNull(recordLog)) {
      //Create an object that has predefined "fake" values
      recordTierCalculationView = FakeTierCalculationProvider.getFakeObject();
      final RecordTierCalculationSummary recordTierCalculationSummary = recordTierCalculationView.getRecordTierCalculationSummary();
      final TierValues tierValues = calculateTierValues(recordLog.getContent());

      //Update only the summary which is what we need for now
      // TODO: 04/01/2022 Update the remaining values
      recordTierCalculationSummary.setEuropeanaRecordId("europeanaRecordId"); // Here use XmlRecordProcessorServiceImpl
      recordTierCalculationSummary.setProviderRecordId(recordId);
      recordTierCalculationSummary.setContentTier(tierValues.getContentTier());
      recordTierCalculationSummary.setMetadataTier(tierValues.getMetadataTier());
      recordTierCalculationSummary.setPortalLink("https://example.com"); // We need the root link from the configuration
      recordTierCalculationSummary.setHarvestedRecordLink("https://example.com"); // We need a proxy controller for this
    }

    return recordTierCalculationView;
  }

  private TierValues calculateTierValues(String xml) {
    final RDF rdf;
    try {
      // Perform the tier classification
      rdf = RdfConversionUtils.convertStringToRdf(xml);
      final RdfWrapper rdfWrapper = new RdfWrapper(rdf);
      RdfTierUtils.setTier(rdf, ClassifierFactory.getMediaClassifier().classify(rdfWrapper));
      RdfTierUtils.setTier(rdf, ClassifierFactory.getMetadataClassifier().classify(rdfWrapper));

      final RdfToFullBeanConverter fullBeanConverter = new RdfToFullBeanConverter();
      final FullBeanImpl fullBean = fullBeanConverter.convertRdfToFullBean(rdfWrapper);
      final List<SolrTier> solrTiers = fullBean.getQualityAnnotations().stream().map(RdfTierUtils::getTier)
          .map(RdfTierUtils::getSolrTier).collect(
              Collectors.toList());
      final String contentTier = solrTiers.stream().filter(solrTier -> solrTier.getTierLabel() == EdmLabel.CONTENT_TIER)
          .findFirst().map(SolrTier::getTierValue).orElse(null);
      final String metadataTier = solrTiers.stream().filter(solrTier -> solrTier.getTierLabel() == EdmLabel.METADATA_TIER)
          .findFirst().map(SolrTier::getTierValue).orElse(null);
      return new TierValues(contentTier, metadataTier);
    } catch (SerializationException | IndexingException e) {
      throw new RecordParsingException(e);
    }
  }

  static class TierValues {

    private final String contentTier;
    private final String metadataTier;

    public TierValues(String contentTier, String metadataTier) {
      this.contentTier = contentTier;
      this.metadataTier = metadataTier;
    }

    public String getContentTier() {
      return contentTier;
    }

    public String getMetadataTier() {
      return metadataTier;
    }
  }
}
