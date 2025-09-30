package eu.europeana.metis.sandbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "dataset_error")
public class DatasetError {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datasetId", referencedColumnName = "datasetId")
  private DatasetEntity datasetEntity;

  @Column(columnDefinition = "TEXT")
  private String message;
  @Column(columnDefinition = "TEXT")
  private String exception;

}
