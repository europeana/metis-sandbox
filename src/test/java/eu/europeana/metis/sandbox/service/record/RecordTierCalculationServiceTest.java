package eu.europeana.metis.sandbox.service.record;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.Execution;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
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
  private ExecutionRecordErrorRepository executionRecordErrorRepository;
  private RecordTierCalculationService recordTierCalculationService;

  @BeforeEach
  void initialize() {
    recordTierCalculationService = Objects.requireNonNullElse(recordTierCalculationService,
        new RecordTierCalculationService(executionRecordRepository, executionRecordErrorRepository,
            PORTAL_PUBLISH_RECORD_BASE_URL));
  }

  @Test
  void calculateTiers_expectSuccess() throws Exception {
    String europeanaRecordString = testUtils.readFileToString(
        Paths.get("record", "media", "europeana_record_with_technical_metadata.xml").toFile().toString());

    final String executionId = "executionId";
    final String datasetId = "datasetId";
    final String externalRecordId = "externalRecordId";
    final String sourceRecordId = "sourceRecordId";
    final String recordId = "recordId";
    Execution execution = new Execution();
    execution.setDatasetId(datasetId);
    execution.setExecutionId(executionId);
    execution.setExecutionName(FullBatchJobType.MEDIA.name());
    ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setExecution(execution);

    ExecutionRecordIdentifier executionRecordIdentifier = new ExecutionRecordIdentifier();
    executionRecordIdentifier.setExternalRecordId(externalRecordId);
    executionRecordIdentifier.setSourceRecordId(sourceRecordId);
    executionRecordIdentifier.setRecordId(recordId);
    executionRecord.setIdentifier(executionRecordIdentifier);

    executionRecord.setRecordData(europeanaRecordString);
    when(executionRecordRepository.findByExecution_DatasetIdAndIdentifier_RecordIdAndExecution_ExecutionName(
        datasetId, recordId, FullBatchJobType.MEDIA.name())).thenReturn(executionRecord);

    final RecordTierCalculationView recordTierCalculationView = recordTierCalculationService.calculateTiers(recordId, datasetId);
    assertNotNull(recordTierCalculationView);
  }

  @Test
  void calculateTiers_NoRecordFoundException() {
    assertThrows(NoRecordFoundException.class, () -> recordTierCalculationService.calculateTiers("recordId", "datasetId"));
  }
}
