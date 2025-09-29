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
 * Entity representing an exception encountered during the processing of an execution record.
 *
 * <p>Uses a composite key, {@link ExecutionRecordIdentifierKey}, to uniquely identify the associated execution record.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record")
public class ExecutionRecordError {

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

  @Column(columnDefinition = "TEXT")
  private String message;
  @Column(columnDefinition = "TEXT")
  private String exception;
}
