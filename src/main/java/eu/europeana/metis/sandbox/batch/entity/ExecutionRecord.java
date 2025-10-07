package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the main execution record entity.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record",
    indexes = {
        @Index(name = "idx_execrecord_recordid", columnList = "recordId"),
        @Index(name = "idx_execrecord_executionrun_recordid", columnList = "execution_run_id, recordId")
    }
)
@SuppressWarnings("javaarchitecture:S7027") // False positive. Valid JPA bi-directional mapping.
public class ExecutionRecord {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private ExecutionRun executionRun;

  @Embedded
  private ExecutionRecordIdentifier identifier;

  @Column(columnDefinition = "TEXT")
  private String recordData;

  @OneToMany(mappedBy = "executionRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<ExecutionRecordWarning> executionRecordWarning = new ArrayList<>();
}

