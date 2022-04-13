package eu.europeana.metis.sandbox.entity.problempatterns;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.hibernate.Hibernate;

@Embeddable
public class DatasetProblemPatternId implements Serializable {

  private static final long serialVersionUID = -3985629933682270973L;
  @Column(name = "execution_point_id", nullable = false)
  private Integer executionPointId;
  @Column(name = "pattern_id", nullable = false, length = 10)
  private String patternId;

  public String getPatternId() {
    return patternId;
  }

  public void setPatternId(String patternId) {
    this.patternId = patternId;
  }

  public Integer getExecutionPointId() {
    return executionPointId;
  }

  public void setExecutionPointId(Integer executionPointId) {
    this.executionPointId = executionPointId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(patternId, executionPointId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    DatasetProblemPatternId entity = (DatasetProblemPatternId) o;
    return Objects.equals(this.patternId, entity.patternId) &&
        Objects.equals(this.executionPointId, entity.executionPointId);
  }
}
