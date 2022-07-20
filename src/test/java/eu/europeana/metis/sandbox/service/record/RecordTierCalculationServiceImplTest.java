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
  private final static String portalPublishRecordBaseUrl = "https://example-domain.org/portal/search?view=grid&q=edm_datasetName:";

  @Mock
  private RecordLogService recordLogServiceMock;
  private RecordTierCalculationServiceImpl recordTierCalculationService;

  @BeforeEach
  public void initialize() {
    recordTierCalculationService = Objects.requireNonNullElse(recordTierCalculationService,
        new RecordTierCalculationServiceImpl(recordLogServiceMock, portalPublishRecordBaseUrl));
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
    final Step mediaProcessStep = Step.MEDIA_PROCESS;
    final RecordLogEntity recordLogEntity = new RecordLogEntity(recordEntity, europeanaRecordString, mediaProcessStep, Status.SUCCESS);
    recordEntity.setId(recordId);
    when(recordLogServiceMock.getRecordLogEntity(providerId, datasetId, Step.MEDIA_PROCESS)).thenReturn(recordLogEntity);


    final RecordTierCalculationView recordTierCalculationView = recordTierCalculationService.calculateTiers(
        providerId, datasetId);
    assertNotNull(recordTierCalculationView);
    assertEquals(recordEntity.getProviderId(),
        recordTierCalculationView.getRecordTierCalculationSummary().getProviderRecordId());
  }

  @Test
  void calculateTiers_NoRecordFoundException(){
    when(recordLogServiceMock.getRecordLogEntity(anyString(), anyString(), any(Step.class))).thenReturn(null);
    assertThrows(NoRecordFoundException.class, ()->recordTierCalculationService.calculateTiers("recordId", "datasetId"));
  }
}