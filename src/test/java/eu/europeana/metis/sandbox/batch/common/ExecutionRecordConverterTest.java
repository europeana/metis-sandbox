package eu.europeana.metis.sandbox.batch.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarning;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExecutionRecordConverterTest {

  @Test
  void convertToExecutionRecordDTO() {
    ExecutionRecordIdentifierKey executionRecordIdentifierKey = new ExecutionRecordIdentifierKey();
    executionRecordIdentifierKey.setDatasetId("datasetId");
    executionRecordIdentifierKey.setExecutionId("executionId");
    executionRecordIdentifierKey.setExecutionName("executionName");
    executionRecordIdentifierKey.setSourceRecordId("sourceRecordId");
    executionRecordIdentifierKey.setRecordId("recordId");

    ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setIdentifier(executionRecordIdentifierKey);
    executionRecord.setRecordData("recordData");
    //    ExecutionRecordWarning executionRecordWarning = new ExecutionRecordWarning();
    //    executionRecordWarning.setMessage("message");DatasetHarvestControllerTest.
    //    executionRecordWarning.setException("exception");
    //    executionRecord.setExecutionRecordWarning(List.of(executionRecordWarning));

    SuccessExecutionRecordDTO successExecutionRecordDTO = ExecutionRecordConverter.convertToExecutionRecordDTO(
        executionRecord);

    assertEquals(executionRecord.getIdentifier().getDatasetId(), successExecutionRecordDTO.getDatasetId());
    assertEquals(executionRecord.getIdentifier().getExecutionId(), successExecutionRecordDTO.getExecutionId());
    assertEquals(executionRecord.getIdentifier().getExecutionName(), successExecutionRecordDTO.getExecutionName());
    assertEquals(executionRecord.getIdentifier().getSourceRecordId(), successExecutionRecordDTO.getSourceRecordId());
    assertEquals(executionRecord.getIdentifier().getRecordId(), successExecutionRecordDTO.getRecordId());
    assertEquals(executionRecord.getRecordData(), successExecutionRecordDTO.getRecordData());
    assertTrue(successExecutionRecordDTO.getExceptionWarnings().isEmpty());
    assertNull(successExecutionRecordDTO.getTierResults());
  }

  @Test
  void convertToExecutionRecord() {
    SuccessExecutionRecordDTO successExecutionRecordDTO = SuccessExecutionRecordDTO.createValidated(b -> b
        .datasetId("datasetId")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .executionId("executionId")
        .executionName("executionName")
        .recordData("recordData")
        .exceptionWarnings(Set.of(new IllegalArgumentException("warning")))
    );

    ExecutionRecord executionRecord = ExecutionRecordConverter.convertToExecutionRecord(successExecutionRecordDTO);

    assertNotNull(executionRecord);
    assertEquals(successExecutionRecordDTO.getDatasetId(), executionRecord.getIdentifier().getDatasetId());
    assertEquals(successExecutionRecordDTO.getExecutionId(), executionRecord.getIdentifier().getExecutionId());
    assertEquals(successExecutionRecordDTO.getExecutionName(), executionRecord.getIdentifier().getExecutionName());
    assertEquals(successExecutionRecordDTO.getSourceRecordId(), executionRecord.getIdentifier().getSourceRecordId());
    assertEquals(successExecutionRecordDTO.getRecordId(), executionRecord.getIdentifier().getRecordId());
    assertEquals(successExecutionRecordDTO.getRecordData(), executionRecord.getRecordData());
    assertEquals(1, executionRecord.getExecutionRecordWarning().size());
    ExecutionRecordWarning executionRecordWarning = executionRecord.getExecutionRecordWarning().getFirst();
    assertTrue(executionRecordWarning.getMessage().contains("warning"));
    assertTrue(executionRecordWarning.getException().contains("IllegalArgumentException"));
  }

  @Test
  void convertToExecutionRecordTierContext() {
    TierResults mockTierResults = mock(TierResults.class);
    when(mockTierResults.getMediaTier()).thenReturn(MediaTier.T0);
    when(mockTierResults.getMetadataTier()).thenReturn(MetadataTier.T0);
    when(mockTierResults.getLicenseType()).thenReturn(LicenseType.OPEN);
    when(mockTierResults.getContentTierBeforeLicenseCorrection()).thenReturn(MediaTier.T1);
    when(mockTierResults.getMetadataTierContextualClasses()).thenReturn(MetadataTier.TA);
    when(mockTierResults.getMetadataTierLanguage()).thenReturn(MetadataTier.TB);
    when(mockTierResults.getMetadataTierEnablingElements()).thenReturn(MetadataTier.TC);

    SuccessExecutionRecordDTO dto = SuccessExecutionRecordDTO.createValidated(b -> b
        .datasetId("datasetId")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .executionId("executionId")
        .executionName("executionName")
        .recordData("recordData")
        .tierResults(mockTierResults)
    );

    Optional<ExecutionRecordTierContext> executionRecordTierContextOptional = ExecutionRecordConverter.convertToExecutionRecordTierContext(
        dto);
    assertTrue(executionRecordTierContextOptional.isPresent());
    ExecutionRecordTierContext executionRecordTierContext = executionRecordTierContextOptional.get();
    assertEquals(mockTierResults.getMediaTier().toString(), executionRecordTierContext.getContentTier());
    assertEquals(mockTierResults.getMetadataTier().toString(), executionRecordTierContext.getMetadataTier());
    assertEquals(mockTierResults.getLicenseType().toString(), executionRecordTierContext.getLicense());
    assertEquals(mockTierResults.getContentTierBeforeLicenseCorrection().toString(),
        executionRecordTierContext.getContentTierBeforeLicenseCorrection());
    assertEquals(mockTierResults.getMetadataTierContextualClasses().toString(),
        executionRecordTierContext.getMetadataTierContextualClasses());
    assertEquals(mockTierResults.getMetadataTierLanguage().toString(), executionRecordTierContext.getMetadataTierLanguage());
    assertEquals(mockTierResults.getMetadataTierEnablingElements().toString(),
        executionRecordTierContext.getMetadataTierEnablingElements());
  }

  @Test
  void convertToExecutionRecordTierContext_ReturnEmptyIfNoTiers() {
    SuccessExecutionRecordDTO successExecutionRecordDTO = SuccessExecutionRecordDTO.createValidated(b -> b
        .datasetId("datasetId")
        .executionId("executionId")
        .executionName("executionName")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .recordData("recordData")
    );

    Optional<ExecutionRecordTierContext> executionRecordTierContext = ExecutionRecordConverter.convertToExecutionRecordTierContext(
        successExecutionRecordDTO);
    assertTrue(executionRecordTierContext.isEmpty());
  }

  @Test
  void converterToExecutionRecordError() {
    IllegalArgumentException illegalArgumentException = new IllegalArgumentException("illegalArgumentMessage");
    FailExecutionRecordDTO failExecutionRecordDTO = FailExecutionRecordDTO.createValidated(b -> b
        .datasetId("datasetId")
        .executionId("executionId")
        .executionName("executionName")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .exception(illegalArgumentException)
    );

    ExecutionRecordError executionRecordError = ExecutionRecordConverter.converterToExecutionRecordError(
        failExecutionRecordDTO);

    assertNotNull(executionRecordError);
    assertEquals(failExecutionRecordDTO.getDatasetId(), executionRecordError.getIdentifier().getDatasetId());
    assertEquals(failExecutionRecordDTO.getExecutionId(), executionRecordError.getIdentifier().getExecutionId());
    assertEquals(failExecutionRecordDTO.getExecutionName(), executionRecordError.getIdentifier().getExecutionName());
    assertEquals(failExecutionRecordDTO.getSourceRecordId(), executionRecordError.getIdentifier().getSourceRecordId());
    assertEquals(failExecutionRecordDTO.getRecordId(), executionRecordError.getIdentifier().getRecordId());
    assertNotNull(executionRecordError.getException());
    assertTrue(executionRecordError.getException().contains("IllegalArgumentException"));
    assertEquals(failExecutionRecordDTO.getException().getMessage(), executionRecordError.getMessage());
  }
}
