package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import java.util.Objects;
import java.util.StringJoiner;

public class Record {

  private final String recordId;
  private final String datasetId;
  private final String datasetName;
  private final Country country;
  private final Language language;
  private final Step step;
  private final Status status;
  private final String content;

  private Record(String recordId, String datasetId, String datasetName,
      Country country, Language language, Step step,
      Status status, String content) {
    this.recordId = recordId;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.country = country;
    this.language = language;
    this.step = step;
    this.status = status;
    this.content = content;
  }

  public static Record from(Record record, Step step) {
    return Record.builder()
        .recordId(record.getRecordId())
        .datasetId(record.getDatasetId())
        .datasetName(record.getDatasetName())
        .content(record.getContent())
        .country(record.getCountry())
        .language(record.getLanguage())
        .status(record.getStatus())
        .step(step)
        .build();
  }

  public static RecordBuilder builder() {
    return new RecordBuilder();
  }

  public String getRecordId() {
    return this.recordId;
  }

  public String getDatasetId() {
    return this.datasetId;
  }

  public String getDatasetName() {
    return this.datasetName;
  }

  public Country getCountry() {
    return this.country;
  }

  public Language getLanguage() {
    return this.language;
  }

  public Step getStep() {
    return this.step;
  }

  public Status getStatus() {
    return this.status;
  }

  public String getContent() {
    return this.content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Record record = (Record) o;
    return recordId.equals(record.recordId) &&
        datasetId.equals(record.datasetId) &&
        datasetName.equals(record.datasetName) &&
        country == record.country &&
        language == record.language &&
        step == record.step &&
        status == record.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(recordId, datasetId, datasetName, country, language, step, status);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Record.class.getSimpleName() + "[", "]")
        .add("recordId='" + recordId + "'")
        .add("datasetId='" + datasetId + "'")
        .add("datasetName='" + datasetName + "'")
        .add("country=" + country)
        .add("language=" + language)
        .add("step=" + step)
        .add("status=" + status)
        .add("content='" + content + "'")
        .toString();
  }

  public static class RecordBuilder {

    private String recordId;
    private String datasetId;
    private String datasetName;
    private Country country;
    private Language language;
    private Step step;
    private Status status;
    private String content;

    public RecordBuilder recordId(String recordId) {
      this.recordId = recordId;
      return this;
    }

    public RecordBuilder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public RecordBuilder datasetName(String datasetName) {
      this.datasetName = datasetName;
      return this;
    }

    public RecordBuilder country(Country country) {
      this.country = country;
      return this;
    }

    public RecordBuilder language(Language language) {
      this.language = language;
      return this;
    }

    public RecordBuilder step(Step step) {
      this.step = step;
      return this;
    }

    public RecordBuilder status(Status status) {
      this.status = status;
      return this;
    }

    public RecordBuilder content(String content) {
      this.content = content;
      return this;
    }

    public Record build() {
      requireNonNull(recordId, "Record id must not be null");
      requireNonNull(datasetId, "Dataset id must not be null");
      requireNonNull(datasetName, "Dataset name id must not be null");
      requireNonNull(country, "Country must not be null");
      requireNonNull(language, "Language must not be null");
      requireNonNull(step, "Step must not be null");
      requireNonNull(status, "Status must not be null");
      requireNonNull(content, "Content must not be null");
      return new Record(recordId, datasetId, datasetName, country, language, step, status, content);
    }
  }
}
