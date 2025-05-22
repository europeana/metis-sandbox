package eu.europeana.metis.sandbox.batch.entity;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionRecordDTO {

  private String datasetId;
  private String executionId;
  private String recordId;
  private String executionName;
  private String recordData;
  private Map<String, String> warnings = Map.of();

  private String contentTier;
  private String contentTierBeforeLicenseCorrection;
  private String metadataTier;
  private String metadataTierLanguage;
  private String metadataTierEnablingElements;
  private String metadataTierContextualClasses;
  private String license;
}
