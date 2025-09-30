package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an execution entity based on a datasetId, executionId and executionName.
 */
@Getter
@Setter
@Entity
@Table(schema = "engine_record",
    indexes = {
        @Index(name = "idx_exec_dataset_execid", columnList = "datasetId, executionId"),
        @Index(name = "idx_exec_dataset_execname", columnList = "datasetId, executionName")
    })
public class Execution {

  @Id
  @GeneratedValue
  private Long id;

  @Column(length = 50)
  private String datasetId;

  @Column(length = 50)
  private String executionId;

  @Column(length = 50)
  private String executionName;

}
