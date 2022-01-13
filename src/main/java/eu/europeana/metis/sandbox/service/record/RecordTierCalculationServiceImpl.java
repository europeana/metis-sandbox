package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.RecordTierCalculationViewGenerator;
import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriTemplate;

/**
 * Service for calculating record tier statistics
 */
@Service
public class RecordTierCalculationServiceImpl implements RecordTierCalculationService {

  private final RecordService recordService;

  private final String providerRecordUrlTemplate;

  private final String portalPublishRecordBaseUrl;

  /**
   * Parameterized constructor
   *
   * @param recordService the record service
   * @param providerRecordUrlTemplate the provider record url template.
   * <p>
   * This string value should conform to {@link UriTemplate}.
   * </p>
   * @param portalPublishRecordBaseUrl the portal publish record base url
   */
  public RecordTierCalculationServiceImpl(RecordService recordService,
      String providerRecordUrlTemplate,
      String portalPublishRecordBaseUrl) {
    this.recordService = recordService;
    this.providerRecordUrlTemplate = providerRecordUrlTemplate;
    this.portalPublishRecordBaseUrl = portalPublishRecordBaseUrl;
  }

  @Override
  public RecordTierCalculationView calculateTiers(RecordIdType recordIdType, String recordId,
      String datasetId) throws NoRecordFoundException {
    final RecordEntity recordLog = recordService.getRecordEntity(recordIdType, recordId, datasetId);

    RecordTierCalculationView recordTierCalculationView;
    if (Objects.nonNull(recordLog)) {
      final String portalPublishRecordUrl = new UriTemplate(this.portalPublishRecordBaseUrl).expand(recordLog.getEuropeanaId())
          .toString();
      final String providerRecordUrl = new UriTemplate(this.providerRecordUrlTemplate).expand(datasetId, recordId, recordIdType)
          .toString();
      final RecordTierCalculationViewGenerator recordTierCalculationViewGenerator = new RecordTierCalculationViewGenerator(
          recordLog.getEuropeanaId(), String.valueOf(recordLog.getId()), recordLog.getContent(), portalPublishRecordUrl,
          providerRecordUrl);
      recordTierCalculationView = recordTierCalculationViewGenerator.generate();
    } else {
      throw new NoRecordFoundException(
          String.format("Record not found for RecordIdType: %s, recordId: %s, datasetId: %s", recordIdType, recordId, datasetId));
    }

    return recordTierCalculationView;
  }

}
