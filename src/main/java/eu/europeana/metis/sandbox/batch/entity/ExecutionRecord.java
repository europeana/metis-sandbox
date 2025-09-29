package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the main execution record entity.
 *
 * <p>The identifier is a composite key provided by the {@link ExecutionRecordIdentifierKey} class.
 * <p>Includes a one-to-many relationship with {@link ExecutionRecordWarning} entities
 * to manage related warning exceptions.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record")
@SuppressWarnings("javaarchitecture:S7027") // False positive. Valid JPA bi-directional mapping.
public class ExecutionRecord {

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
  private String recordData;

  @OneToMany(mappedBy = "executionRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<ExecutionRecordWarning> executionRecordWarning = new ArrayList<>();
}

