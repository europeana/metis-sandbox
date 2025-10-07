package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an external identifier associated with an execution record.
 * <p>
 * It includes a composite key for uniquely identifying the record and a flag indicating if the record is marked as deleted.
 * Mostly used for oai harvesting.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record",
    indexes = {
        @Index(name = "idx_execrecordextid_executionrun_id", columnList = "execution_run_id, id")
    }
)
public class ExecutionRecordExternalIdentifier {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private ExecutionRun executionRun;

  @Column(length = 300)
  private String externalRecordId;

  private boolean isDeleted;
}
