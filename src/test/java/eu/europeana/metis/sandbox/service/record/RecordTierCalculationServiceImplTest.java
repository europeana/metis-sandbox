package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordTierCalculationServiceImplTest {

  private final TestUtils testUtils = new TestUtils();
  private final static String providerRecordUrlTemplate = "http://localhost:8080/dataset/{datasetId}/record?recordId={recordId}&recordIdType={recordIdType}";
  private final static String portalPublishRecordBaseUrl = "https://example-domain.org/portal/search?view=grid&q=edm_datasetName:";

  @Mock
  private RecordService recordServiceMock;
  @Mock
  private RecordLogService recordLogService;
  private RecordTierCalculationServiceImpl recordTierCalculationService;

  @BeforeEach
  public void initialize() {
    recordTierCalculationService = Objects.requireNonNullElse(recordTierCalculationService,
        new RecordTierCalculationServiceImpl(recordLogService, providerRecordUrlTemplate,
            portalPublishRecordBaseUrl));
  }

  @Test
  void calculateTiers_expectSuccess() throws Exception {
    String europeanaRecordString = testUtils.readFileToString(
        Paths.get("record", "media", "europeana_record_with_technical_metadata.xml").toFile().toString());

    final Long recordId = 1L;
    final String datasetId = "datasetId";
    final String europeanaId = "europeanaId";
    final String providerId = "providerId";
    final RecordEntity recordEntity = new RecordEntity(europeanaId, providerId, datasetId);
    final RecordLogEntity recordLogEntity = new RecordLogEntity(recordEntity, europeanaRecordString, Step.MEDIA_PROCESS, Status.SUCCESS);
    recordEntity.setId(recordId);
    when(recordLogService.getRecordLogEntity(RecordIdType.PROVIDER_ID, String.valueOf(recordId), datasetId)).thenReturn(recordLogEntity);
    final RecordTierCalculationView recordTierCalculationView = recordTierCalculationService.calculateTiers(
        RecordIdType.PROVIDER_ID, String.valueOf(recordId), datasetId);
    assertNotNull(recordTierCalculationView);
    assertEquals(String.valueOf(recordEntity.getId()),
        recordTierCalculationView.getRecordTierCalculationSummary().getProviderRecordId());
  }

  @Test
  void calculateTiers_NoRecordFoundException(){
    when(recordLogService.getRecordLogEntity(any(RecordIdType.class), anyString(), anyString())).thenReturn(null);
    assertThrows(NoRecordFoundException.class, ()->recordTierCalculationService.calculateTiers(
        RecordIdType.PROVIDER_ID, "recordId", "datasetId"));
  }
}