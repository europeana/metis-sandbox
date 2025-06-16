package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordTierCalculationServiceTest {

  private final TestUtils testUtils = new TestUtils();
  private static final String PORTAL_PUBLISH_RECORD_BASE_URL = "https://example-domain.org/portal/search?view=grid&q=edm_datasetName:";

  @Mock
  private ExecutionRecordRepository executionRecordRepository;
  @Mock
  private ExecutionRecordExceptionRepository executionRecordExceptionRepository;
  private RecordTierCalculationService recordTierCalculationService;

  @BeforeEach
  public void initialize() {
    recordTierCalculationService = Objects.requireNonNullElse(recordTierCalculationService,
        new RecordTierCalculationService(executionRecordRepository, executionRecordExceptionRepository,
            PORTAL_PUBLISH_RECORD_BASE_URL));
  }

  @Test
  void calculateTiers_expectSuccess() throws Exception {
    String europeanaRecordString = testUtils.readFileToString(
        Paths.get("record", "media", "europeana_record_with_technical_metadata.xml").toFile().toString());

    final String executionId = "executionId";
    final String datasetId = "datasetId";
    final String recordId = "recordId";
    ExecutionRecordIdentifierKey executionRecordIdentifierKey = new ExecutionRecordIdentifierKey();
    executionRecordIdentifierKey.setDatasetId(datasetId);
    executionRecordIdentifierKey.setRecordId(recordId);
    executionRecordIdentifierKey.setExecutionName(FullBatchJobType.MEDIA.name());
    executionRecordIdentifierKey.setExecutionId(executionId);
    ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setIdentifier(executionRecordIdentifierKey);
    executionRecord.setRecordData(europeanaRecordString);
    when(executionRecordRepository.findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(
        datasetId, recordId, FullBatchJobType.MEDIA.name())).thenReturn(executionRecord);

    final RecordTierCalculationView recordTierCalculationView = recordTierCalculationService.calculateTiers(recordId, datasetId);
    assertNotNull(recordTierCalculationView);
  }

  @Test
  void calculateTiers_NoRecordFoundException() {
    assertThrows(NoRecordFoundException.class, () -> recordTierCalculationService.calculateTiers("recordId", "datasetId"));
  }
}
