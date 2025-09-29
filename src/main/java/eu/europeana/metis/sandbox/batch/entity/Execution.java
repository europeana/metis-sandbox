package eu.europeana.metis.sandbox.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "engine_record")
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
