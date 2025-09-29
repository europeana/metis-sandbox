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
import eu.europeana.metis.sandbox.batch.dto.ExceptionInfoDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.Execution;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarning;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExecutionRecordConverterTest {

  @Test
  void convertToExecutionRecordDTO() {
    Execution execution = new Execution();
    execution.setDatasetId("datasetId");
    execution.setExecutionId("executionId");
    execution.setExecutionName("executionName");

    ExecutionRecord executionRecord = new ExecutionRecord();
    executionRecord.setExecution(execution);
    executionRecord.setExternalRecordId("externalRecordId");
    executionRecord.setSourceRecordId("sourceRecordId");
    executionRecord.setRecordId("recordId");
    executionRecord.setRecordData("recordData");
    ExecutionRecordWarning executionRecordWarning = new ExecutionRecordWarning();
    executionRecordWarning.setMessage("message");
    executionRecordWarning.setException("exception");
    executionRecord.setExecutionRecordWarning(List.of(executionRecordWarning));

    SuccessExecutionRecordDTO successExecutionRecordDTO = ExecutionRecordConverter.convertToExecutionRecordDTO(
        executionRecord);

    assertEquals(executionRecord.getExecution().getDatasetId(), successExecutionRecordDTO.getDatasetId());
    assertEquals(executionRecord.getExecution().getExecutionId(), successExecutionRecordDTO.getExecutionId());
    assertEquals(executionRecord.getExecution().getExecutionName(), successExecutionRecordDTO.getExecutionName());
    assertEquals(executionRecord.getExternalRecordId(), successExecutionRecordDTO.getExternalRecordId());
    assertEquals(executionRecord.getSourceRecordId(), successExecutionRecordDTO.getSourceRecordId());
    assertEquals(executionRecord.getRecordId(), successExecutionRecordDTO.getRecordId());
    assertEquals(executionRecord.getRecordData(), successExecutionRecordDTO.getRecordData());
    Set<String> entityWarningSet = executionRecord.getExecutionRecordWarning().stream()
                                                  .map(w -> w.getMessage() + "-" + w.getException())
                                                  .collect(Collectors.toSet());

    Set<String> dtoWarningSet = successExecutionRecordDTO.getExceptionWarnings().stream()
                                                         .map(d -> d.getMessage() + "-" + d.getStackTrace())
                                                         .collect(Collectors.toSet());
    assertEquals(entityWarningSet, dtoWarningSet, "Warning sets do not match");
    assertNull(successExecutionRecordDTO.getTierResults());
  }

  @Test
  void convertToExecutionRecord() {
    IllegalArgumentException illegalArgumentException = new IllegalArgumentException("warning");
    SuccessExecutionRecordDTO successExecutionRecordDTO = SuccessExecutionRecordDTO.createValidated(b -> b
        .datasetId("datasetId")
        .externalRecordId("externalRecordId")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .executionId("executionId")
        .executionName("executionName")
        .recordData("recordData")
        .exceptionWarnings(Set.of(ExceptionInfoDTO.from(illegalArgumentException)))
    );
    Execution execution = new Execution();
    execution.setDatasetId("datasetId");
    execution.setExecutionId("executionId");
    execution.setExecutionName("executionName");

    ExecutionRecord executionRecord = ExecutionRecordConverter.convertToExecutionRecord(successExecutionRecordDTO, execution);

    assertNotNull(executionRecord);
    assertEquals(successExecutionRecordDTO.getDatasetId(), executionRecord.getExecution().getDatasetId());
    assertEquals(successExecutionRecordDTO.getExecutionId(), executionRecord.getExecution().getExecutionId());
    assertEquals(successExecutionRecordDTO.getExecutionName(), executionRecord.getExecution().getExecutionName());
    assertEquals(successExecutionRecordDTO.getExternalRecordId(), executionRecord.getExternalRecordId());
    assertEquals(successExecutionRecordDTO.getSourceRecordId(), executionRecord.getSourceRecordId());
    assertEquals(successExecutionRecordDTO.getRecordId(), executionRecord.getRecordId());
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

    SuccessExecutionRecordDTO successExecutionRecordDTO = SuccessExecutionRecordDTO.createValidated(b -> b
        .datasetId("datasetId")
        .externalRecordId("externalRecordId")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .executionId("executionId")
        .executionName("executionName")
        .recordData("recordData")
        .tierResults(mockTierResults)
    );
    Execution execution = new Execution();
    execution.setDatasetId("datasetId");
    execution.setExecutionId("executionId");
    execution.setExecutionName("executionName");

    Optional<ExecutionRecordTierContext> executionRecordTierContextOptional =
        ExecutionRecordConverter.convertToExecutionRecordTierContext(successExecutionRecordDTO, execution);
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
        .externalRecordId("externalRecordId")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .recordData("recordData")
    );
    Execution execution = new Execution();
    execution.setDatasetId("datasetId");
    execution.setExecutionId("executionId");
    execution.setExecutionName("executionName");

    Optional<ExecutionRecordTierContext> executionRecordTierContext = ExecutionRecordConverter.convertToExecutionRecordTierContext(
        successExecutionRecordDTO, execution);
    assertTrue(executionRecordTierContext.isEmpty());
  }

  @Test
  void converterToExecutionRecordError() {
    IllegalArgumentException illegalArgumentException = new IllegalArgumentException("illegalArgumentMessage");
    FailExecutionRecordDTO failExecutionRecordDTO = FailExecutionRecordDTO.createValidated(b -> b
        .datasetId("datasetId")
        .executionId("executionId")
        .executionName("executionName")
        .externalRecordId("externalRecordId")
        .sourceRecordId("sourceRecordId")
        .recordId("recordId")
        .exceptionInfoDTO(ExceptionInfoDTO.from(illegalArgumentException))
    );
    Execution execution = new Execution();
    execution.setDatasetId("datasetId");
    execution.setExecutionId("executionId");
    execution.setExecutionName("executionName");

    ExecutionRecordError executionRecordError = ExecutionRecordConverter.converterToExecutionRecordError(
        failExecutionRecordDTO, execution);

    assertNotNull(executionRecordError);
    assertEquals(failExecutionRecordDTO.getDatasetId(), executionRecordError.getExecution().getDatasetId());
    assertEquals(failExecutionRecordDTO.getExecutionId(), executionRecordError.getExecution().getExecutionId());
    assertEquals(failExecutionRecordDTO.getExecutionName(), executionRecordError.getExecution().getExecutionName());
    assertEquals(failExecutionRecordDTO.getExternalRecordId(), executionRecordError.getExternalRecordId());
    assertEquals(failExecutionRecordDTO.getSourceRecordId(), executionRecordError.getSourceRecordId());
    assertEquals(failExecutionRecordDTO.getRecordId(), executionRecordError.getRecordId());
    assertNotNull(executionRecordError.getException());
    assertTrue(executionRecordError.getException().contains("IllegalArgumentException"));
    assertEquals(failExecutionRecordDTO.getExceptionInfoDTO().getMessage(), executionRecordError.getMessage());
  }
}
