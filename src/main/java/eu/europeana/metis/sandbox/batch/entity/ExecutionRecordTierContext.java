package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing the context of tier-based information for an execution record.
 *
 * <p>Provides details such as content and metadata tiers that are associated with an execution record.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record", indexes = {
    @Index(name = "exec_rec_tier_dataset_id_execution_id_idx", columnList = "datasetId, executionId")})
public class ExecutionRecordTierContext implements HasExecutionRecordIdAccess<ExecutionRecordIdentifierKey> {

  @EmbeddedId
  private ExecutionRecordIdentifierKey identifier;

  @Column(length = 1)
  protected String contentTier;

  @Column(length = 1)
  protected String contentTierBeforeLicenseCorrection;

  @Column(length = 1)
  protected String metadataTier;

  @Column(length = 1)
  protected String metadataTierLanguage;

  @Column(length = 1)
  protected String metadataTierEnablingElements;

  @Column(length = 1)
  protected String metadataTierContextualClasses;

  @Column(length = 20)
  protected String license;
}

