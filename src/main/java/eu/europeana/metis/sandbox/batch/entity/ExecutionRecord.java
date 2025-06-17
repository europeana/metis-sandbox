package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
 * <p>Includes a one-to-many relationship with {@link ExecutionRecordWarningException} entities
 * to manage related warning exceptions.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record", indexes = {
    @Index(name = "exec_rec_dataset_id_execution_id_idx", columnList = "datasetId, executionId")})
public class ExecutionRecord implements HasExecutionRecordIdAccess<ExecutionRecordIdentifierKey> {

  @EmbeddedId
  private ExecutionRecordIdentifierKey identifier;

  @Column(columnDefinition = "TEXT")
  private String recordData;

  @OneToMany(mappedBy = "executionRecord", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExecutionRecordWarningException> executionRecordWarningException = new ArrayList<>();
}

