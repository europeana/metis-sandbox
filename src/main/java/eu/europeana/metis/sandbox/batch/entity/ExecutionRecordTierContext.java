package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
@Table(schema = "engine_record")
public class ExecutionRecordTierContext {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private Execution execution;

  @Column(length = 300)
  private String externalRecordId;

  @Column(length = 300)
  private String sourceRecordId;

  @Column(length = 300)
  private String recordId;

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

