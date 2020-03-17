package eu.europeana.metis.sandbox.entity;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Step;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class RecordLogEntityKey implements Serializable {

  private String id;

  private String datasetId;

  @Enumerated(EnumType.STRING)
  private Step step;

  private RecordLogEntityKey(String id, String datasetId, Step step) {
    requireNonNull(id, "Id must not be null");
    requireNonNull(datasetId, "Dataset id must not be null");
    requireNonNull(step, "Step must not be null");
    this.id = id;
    this.datasetId = datasetId;
    this.step = step;
  }

  public RecordLogEntityKey() {
  }

  public static RecordLogEntityKeyBuilder builder() {
    return new RecordLogEntityKeyBuilder();
  }

  public String getId() {
    return this.id;
  }

  public String getDatasetId() {
    return this.datasetId;
  }

  public Step getStep() {
    return this.step;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public void setStep(Step step) {
    this.step = step;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordLogEntityKey that = (RecordLogEntityKey) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(datasetId, that.datasetId) &&
        step == that.step;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, datasetId, step);
  }

  public static class RecordLogEntityKeyBuilder {

    private String id;
    private String datasetId;
    private Step step;

    public RecordLogEntityKey.RecordLogEntityKeyBuilder id(String id) {
      this.id = id;
      return this;
    }

    public RecordLogEntityKey.RecordLogEntityKeyBuilder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public RecordLogEntityKey.RecordLogEntityKeyBuilder step(Step step) {
      this.step = step;
      return this;
    }

    public RecordLogEntityKey build() {
      return new RecordLogEntityKey(id, datasetId, step);
    }
  }
}
