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

/**
 * Represents an error associated with a dataset.
 * <p>
 * This entity is used to store information about errors that occur during dataset processing. This usually represents a failure
 * at the dataset level and not individual errors of particular records.
 */
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
