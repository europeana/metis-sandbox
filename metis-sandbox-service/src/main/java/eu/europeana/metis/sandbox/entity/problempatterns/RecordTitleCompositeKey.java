package eu.europeana.metis.sandbox.entity.problempatterns;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.Hibernate;

/**
 * Composite id class for {@link RecordTitle}
 */
@Embeddable
public class RecordTitleCompositeKey implements Serializable {

  private static final long serialVersionUID = -6429083329090813045L;
  @Column(name = "execution_point_id", nullable = false)
  private Integer executionPointId;
  @Column(name = "record_id", nullable = false)
  private String recordId;
  @Column(name = "title", nullable = false)
  private String title;

  public RecordTitleCompositeKey() {
    //Required for JPA
  }

  /**
   * Constructor with required parameters.
   *
   * @param executionPointId the execution point id
   * @param recordId the pattern id
   * @param title the title
   */
  public RecordTitleCompositeKey(Integer executionPointId, String recordId, String title) {
    this.executionPointId = executionPointId;
    this.recordId = recordId;
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public Integer getExecutionPointId() {
    return executionPointId;
  }

  public void setExecutionPointId(Integer executionPointId) {
    this.executionPointId = executionPointId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordId, executionPointId, title);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    RecordTitleCompositeKey entity = (RecordTitleCompositeKey) o;
    return Objects.equals(this.recordId, entity.recordId) &&
        Objects.equals(this.executionPointId, entity.executionPointId) &&
        Objects.equals(this.title, entity.title);
  }
}
