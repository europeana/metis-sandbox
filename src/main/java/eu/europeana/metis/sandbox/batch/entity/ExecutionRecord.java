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

@Getter
@Setter
@Entity
@Table(schema = "batch-framework", indexes = {@Index(name = "exec_rec_dataset_id_execution_id_idx", columnList = "datasetId, executionId")})
public class ExecutionRecord implements HasExecutionRecordIdAccess<ExecutionRecordIdentifierKey> {

  @EmbeddedId
  private ExecutionRecordIdentifierKey identifier;

  @Column(columnDefinition = "TEXT")
  private String recordData;

  @OneToMany(mappedBy = "executionRecord", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ExecutionRecordWarningException> executionRecordWarningException = new ArrayList<>();
}

