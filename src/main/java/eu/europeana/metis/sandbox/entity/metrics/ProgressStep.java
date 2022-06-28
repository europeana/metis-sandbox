package eu.europeana.metis.sandbox.entity.metrics;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity class for keep track of dataset step metrics
 */
@Entity
@Table(schema="metrics", name="progres_per_step")
public class ProgressStep {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer metric_id;

  @Column(name = "step", nullable = false)
  private String step;

  @Column(name = "total", nullable = false)
  private Integer total;

  @Column(name = "success", nullable = false)
  private Integer success;

  @Column(name = "fail", nullable = false)
  private Integer fail;

  @Column(name = "warn", nullable = false)
  private Integer warn;
}
